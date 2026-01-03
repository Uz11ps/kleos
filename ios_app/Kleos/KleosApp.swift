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
        print("🔗 Received Deep Link: \(url.absoluteString)")
        let components = URLComponents(url: url, resolvingAgainstBaseURL: true)
        
        if url.host == "verified" {
            if let jwt = components?.queryItems?.first(where: { $0.name == "jwt" })?.value {
                print("🔗 Received JWT from Safari, processing...")
                
                // Просто сохраняем новый токен (SessionManager сам почистит гостевые данные)
                sessionManager.saveToken(jwt)
                
                Task {
                    do {
                        // Небольшая пауза для завершения записи в UserDefaults
                        try await Task.sleep(nanoseconds: 300_000_000)
                        
                        let profile = try await ApiClient.shared.getProfile()
                        await MainActor.run {
                            sessionManager.saveUser(fullName: profile.fullName, email: profile.email, role: profile.role)
                            print("✅ Deep Link: Session updated successfully")
                        }
                    } catch {
                        print("❌ Deep Link: Profile load error: \(error)")
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
