import Foundation
import Combine

class SessionManager: ObservableObject {
    static let shared = SessionManager()
    
    @Published var isLoggedIn: Bool = false
    @Published var currentUser: UserProfile?
    @Published var isUserGuest: Bool = true
    
    // Переменная для глубоких ссылок (чтобы открывать профиль и т.д.)
    @Published var deepLinkAction: DeepLinkAction?
    
    enum DeepLinkAction {
        case openProfile
    }
    
    private let userDefaults = UserDefaults.standard
    private let tokenKey = "kleos_auth_token"
    private let userEmailKey = "kleos_user_email"
    private let userFullNameKey = "kleos_user_full_name"
    private let userIdKey = "kleos_user_id"
    private let userRoleKey = "kleos_user_role"
    
    private var _token: String?

    private init() {
        _token = userDefaults.string(forKey: tokenKey)
        checkLoginStatus()
    }
    
    func checkLoginStatus() {
        let email = userDefaults.string(forKey: userEmailKey)
        _token = userDefaults.string(forKey: tokenKey)
        
        isLoggedIn = _token != nil && email != nil
        isUserGuest = determineGuestStatus()
        
        if isLoggedIn {
            loadCurrentUser()
        }
    }
    
    private func determineGuestStatus() -> Bool {
        let email = userDefaults.string(forKey: userEmailKey)
        if let t = _token, t.contains(".") { return false }
        if email == "guest@local" { return true }
        return _token == nil || email == nil
    }
    
    func isGuest() -> Bool {
        return isUserGuest
    }
    
    func saveToken(_ token: String) {
        print("🔑 SessionManager: Saving token...")
        self._token = token
        userDefaults.set(token, forKey: tokenKey)
        userDefaults.synchronize()
        
        // Сразу определяем статус
        let isRealUser = token.contains(".")
        if isRealUser {
            print("✅ SessionManager: JWT Token detected")
            if userDefaults.string(forKey: userEmailKey) == "guest@local" {
                userDefaults.removeObject(forKey: userEmailKey)
                userDefaults.removeObject(forKey: userFullNameKey)
            }
        }
        
        DispatchQueue.main.async {
            self.isUserGuest = !isRealUser
            self.isLoggedIn = true
            self.objectWillChange.send()
        }
    }
    
    func getToken() -> String? {
        return _token ?? userDefaults.string(forKey: tokenKey)
    }
    
    func saveUser(fullName: String, email: String, role: String? = nil) {
        print("👤 SessionManager: Saving user info (\(email))")
        userDefaults.set(fullName, forKey: userFullNameKey)
        userDefaults.set(email, forKey: userEmailKey)
        if let role = role {
            userDefaults.set(role, forKey: userRoleKey)
        }
        userDefaults.synchronize()
        
        DispatchQueue.main.async {
            self.isUserGuest = self.determineGuestStatus()
            self.isLoggedIn = true
            self.loadCurrentUser()
            self.objectWillChange.send()
        }
    }
    
    func loadCurrentUser() {
        guard let email = userDefaults.string(forKey: userEmailKey),
              let fullName = userDefaults.string(forKey: userFullNameKey) else {
            return
        }
        
        currentUser = UserProfile(
            id: userDefaults.string(forKey: userIdKey) ?? "0",
            email: email,
            fullName: fullName,
            role: userDefaults.string(forKey: userRoleKey) ?? "user",
            phone: nil, course: nil, speciality: nil, status: nil, university: nil,
            payment: nil, penalties: nil, notes: nil, studentId: nil,
            emailVerified: true, avatarUrl: nil
        )
    }
    
    func logout() {
        print("🚪 SessionManager: Logging out and cleaning data")
        _token = nil
        userDefaults.removeObject(forKey: tokenKey)
        userDefaults.removeObject(forKey: userEmailKey)
        userDefaults.removeObject(forKey: userFullNameKey)
        userDefaults.removeObject(forKey: userRoleKey)
        userDefaults.removeObject(forKey: userIdKey)
        userDefaults.synchronize()
        
        DispatchQueue.main.async {
            self.isLoggedIn = false
            self.isUserGuest = true
            self.currentUser = nil
            self.objectWillChange.send()
        }
    }
}
