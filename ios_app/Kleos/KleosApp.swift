import SwiftUI
#if os(iOS)
import UIKit
#endif

@main
struct KleosApp: App {
    @StateObject private var sessionManager = SessionManager.shared
    
    init() {
        setupAppearance()
    }
    
    var body: some Scene {
        WindowGroup {
            SplashView()
                .environmentObject(sessionManager)
                .onOpenURL { url in
                    handleDeepLink(url)
                }
        }
    }
    
    private func handleDeepLink(_ url: URL) {
        print("🔗 Deep Link: \(url.absoluteString)")
        let components = URLComponents(url: url, resolvingAgainstBaseURL: true)
        
        if url.host == "verified" {
            if let jwt = components?.queryItems?.first(where: { $0.name == "jwt" })?.value {
                print("🔗 Received JWT from Safari")
                
                // ПРИНУДИТЕЛЬНАЯ ОЧИСТКА СТАРОЙ СЕССИИ
                sessionManager.logout()
                
                // Сохраняем новый токен
                sessionManager.saveToken(jwt)
                
                Task {
                    do {
                        // Ждем 0.5 сек, чтобы все части приложения увидели новый токен
                        try await Task.sleep(nanoseconds: 500_000_000)
                        
                        let profile = try await ApiClient.shared.getProfile()
                        await MainActor.run {
                            sessionManager.saveUser(fullName: profile.fullName, email: profile.email, role: profile.role)
                            print("✅ Session updated successfully")
                        }
                    } catch {
                        print("❌ Profile load error (expected if too fast): \(error)")
                    }
                }
            }
        }
    }
    
    private func setupAppearance() {
        #if os(iOS)
        let appearance = UINavigationBarAppearance()
        appearance.configureWithOpaqueBackground()
        appearance.backgroundColor = UIColor(Color(hex: "0E080F"))
        appearance.titleTextAttributes = [.foregroundColor: UIColor.white]
        UINavigationBar.appearance().standardAppearance = appearance
        UINavigationBar.appearance().scrollEdgeAppearance = appearance
        #endif
    }
}
