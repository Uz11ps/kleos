import Foundation
import Combine

class SessionManager: ObservableObject {
    static let shared = SessionManager()
    
    @Published var isLoggedIn: Bool = false
    @Published var currentUser: UserProfile?
    @Published var isUserGuest: Bool = true
    
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
        isLoggedIn = _token != nil && email != nil
        isUserGuest = determineGuestStatus()
        
        if isLoggedIn {
            loadCurrentUser()
        }
    }
    
    private func determineGuestStatus() -> Bool {
        let email = userDefaults.string(forKey: userEmailKey)
        // Если токен содержит точки - это реальный JWT (не гость)
        if let t = _token, t.contains(".") { return false }
        // Если email гостевой
        if email == "guest@local" { return true }
        // В остальных случаях (нет токена или email) - считаем гостем для безопасности
        return _token == nil || email == nil
    }
    
    func isGuest() -> Bool {
        return isUserGuest
    }
    
    func saveToken(_ token: String) {
        self._token = token
        userDefaults.set(token, forKey: tokenKey)
        
        // При переходе из гостя в реального юзера (JWT) - чистим старые данные
        if token.contains(".") {
            if userDefaults.string(forKey: userEmailKey) == "guest@local" {
                userDefaults.removeObject(forKey: userEmailKey)
                userDefaults.removeObject(forKey: userFullNameKey)
            }
        }
        
        isUserGuest = determineGuestStatus()
        isLoggedIn = true
        objectWillChange.send()
    }
    
    func getToken() -> String? {
        return _token
    }
    
    func saveUser(fullName: String, email: String, role: String? = nil) {
        userDefaults.set(fullName, forKey: userFullNameKey)
        userDefaults.set(email, forKey: userEmailKey)
        if let role = role {
            userDefaults.set(role, forKey: userRoleKey)
        }
        
        isUserGuest = determineGuestStatus()
        isLoggedIn = true
        loadCurrentUser()
        objectWillChange.send()
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
        _token = nil
        userDefaults.removeObject(forKey: tokenKey)
        userDefaults.removeObject(forKey: userEmailKey)
        userDefaults.removeObject(forKey: userFullNameKey)
        userDefaults.removeObject(forKey: userRoleKey)
        
        isLoggedIn = false
        isUserGuest = true
        currentUser = nil
        objectWillChange.send()
    }
}
