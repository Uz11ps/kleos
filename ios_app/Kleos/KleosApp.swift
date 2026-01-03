import SwiftUI
#if os(iOS)
import UIKit
#endif

@main
struct KleosApp: App {
    @StateObject private var sessionManager = SessionManager.shared
    
    init() {
        // Setup app appearance
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
        
        // kleos://verify?token=...
        guard url.scheme == "kleos", url.host == "verify" else { return }
        
        let components = URLComponents(url: url, resolvingAgainstBaseURL: true)
        let token = components?.queryItems?.first(where: { $0.name == "token" })?.value
        
        if let token = token {
            print("🔗 Verification token found: \(token)")
            Task {
                do {
                    let response = try await ApiClient.shared.verifyConsume(token: token)
                    if let newToken = response.token, let user = response.user {
                        await MainActor.run {
                            sessionManager.saveToken(newToken)
                            sessionManager.saveUser(fullName: user.fullName, email: user.email, role: user.role)
                            print("✅ Deep Link login successful")
                        }
                    }
                } catch {
                    print("❌ Deep Link verification failed: \(error)")
                }
            }
        }
    }
    
    private func setupAppearance() {
        #if os(iOS)
        // Navigation bar appearance
        let navBarAppearance = UINavigationBarAppearance()
        navBarAppearance.configureWithOpaqueBackground()
        navBarAppearance.backgroundColor = UIColor(Color.kleosBackground)
        navBarAppearance.titleTextAttributes = [.foregroundColor: UIColor.white]
        navBarAppearance.largeTitleTextAttributes = [.foregroundColor: UIColor.white]
        
        UINavigationBar.appearance().standardAppearance = navBarAppearance
        UINavigationBar.appearance().scrollEdgeAppearance = navBarAppearance
        UINavigationBar.appearance().tintColor = .white
        
        // Tab bar appearance
        let tabBarAppearance = UITabBarAppearance()
        tabBarAppearance.configureWithOpaqueBackground()
        tabBarAppearance.backgroundColor = UIColor(Color.kleosBackground)
        
        UITabBar.appearance().standardAppearance = tabBarAppearance
        UITabBar.appearance().scrollEdgeAppearance = tabBarAppearance
        #endif
    }
}

