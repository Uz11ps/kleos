import Foundation
import Combine

class SessionManager: ObservableObject {
    static let shared = SessionManager()
    
    @Published var isLoggedIn: Bool = false
    @Published var currentUser: UserProfile?
    
    private let userDefaults = UserDefaults.standard
    private let tokenKey = "kleos_auth_token"
    private let userEmailKey = "kleos_user_email"
    private let userFullNameKey = "kleos_user_full_name"
    private let userIdKey = "kleos_user_id"
    private let userRoleKey = "kleos_user_role"
    
    private init() {
        checkLoginStatus()
    }
    
    func checkLoginStatus() {
        let token = getToken()
        let email = userDefaults.string(forKey: userEmailKey)
        isLoggedIn = token != nil && email != nil
        if isLoggedIn {
            loadCurrentUser()
        }
    }
    
    func saveToken(_ token: String) {
        // Если мы переходим из гостя (UUID) в реального пользователя (JWT)
        if isGuest() && token.contains(".") {
            logout()
        }
        userDefaults.set(token, forKey: tokenKey)
        isLoggedIn = true
        objectWillChange.send()
    }
    
    func getToken() -> String? {
        return userDefaults.string(forKey: tokenKey)
    }
    
    func saveUser(fullName: String, email: String, role: String? = nil) {
        userDefaults.set(fullName, forKey: userFullNameKey)
        userDefaults.set(email, forKey: userEmailKey)
        if let role = role {
            userDefaults.set(role, forKey: userRoleKey)
        }
        isLoggedIn = true
        loadCurrentUser()
        objectWillChange.send()
    }
    
    func isGuest() -> Bool {
        let email = userDefaults.string(forKey: userEmailKey)
        let token = getToken()
        // Гость если: email guest@local ИЛИ нет JWT токена
        return email == "guest@local" || token == nil || !(token?.contains(".") ?? false)
    }
    
    func loadCurrentUser() {
        guard let email = userDefaults.string(forKey: userEmailKey),
              let fullName = userDefaults.string(forKey: userFullNameKey) else {
            currentUser = nil
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
        userDefaults.removeObject(forKey: tokenKey)
        userDefaults.removeObject(forKey: userEmailKey)
        userDefaults.removeObject(forKey: userFullNameKey)
        userDefaults.removeObject(forKey: userRoleKey)
        isLoggedIn = false
        currentUser = nil
        objectWillChange.send()
    }
}
