import SwiftUI
#if os(iOS)
import UIKit
#endif

@main
struct KleosApp: App {
    @StateObject private var sessionManager = SessionManager.shared
    
    init() {
        setupAppearance()
        LocalNotificationManager.shared.requestPermissionIfNeeded()
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
        
        // Обработка deep link после верификации email (из Safari)
        if url.host == "verified" {
            if let jwt = components?.queryItems?.first(where: { $0.name == "jwt" })?.value {
                print("🔗 Received JWT from Safari, processing...")
                
                // Сохраняем токен синхронно
                sessionManager.saveToken(jwt)
                
                Task {
                    do {
                        // Небольшая пауза для завершения записи в UserDefaults
                        try await Task.sleep(nanoseconds: 500_000_000)
                        
                        // Загружаем профиль пользователя
                        let profile = try await ApiClient.shared.getProfile()
                        await MainActor.run {
                            sessionManager.saveUser(fullName: profile.fullName, email: profile.email, role: profile.role)
                            print("✅ Deep Link: Session updated successfully")
                        }
                    } catch {
                        print("❌ Deep Link: Profile load error: \(error)")
                        // Если не удалось загрузить профиль, пробуем еще раз через секунду
                        try? await Task.sleep(nanoseconds: 1_000_000_000)
                        Task {
                            do {
                                let profile = try await ApiClient.shared.getProfile()
                                await MainActor.run {
                                    sessionManager.saveUser(fullName: profile.fullName, email: profile.email, role: profile.role)
                                    print("✅ Deep Link: Session updated successfully (retry)")
                                }
                            } catch {
                                print("❌ Deep Link: Profile load error (retry): \(error)")
                            }
                        }
                    }
                }
            }
        }
        
        // Обработка deep link с токеном верификации (из письма)
        if url.host == "verify" {
            if let token = components?.queryItems?.first(where: { $0.name == "token" })?.value {
                print("🔗 Received verification token, consuming...")
                
                Task {
                    do {
                        // Вызываем API для получения JWT токена
                        let response = try await ApiClient.shared.verifyConsume(token: token)
                        
                        if let jwt = response.token, let user = response.user {
                            await MainActor.run {
                                // Сохраняем JWT токен и данные пользователя
                                sessionManager.saveToken(jwt)
                                sessionManager.saveUser(fullName: user.fullName, email: user.email, role: user.role)
                                print("✅ Verification token consumed, session updated")
                            }
                        }
                    } catch {
                        print("❌ Failed to consume verification token: \(error)")
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
