import Foundation
import Combine

class SessionManager: ObservableObject {
    static let shared = SessionManager()
    
    @Published var isLoggedIn: Bool = false
    @Published var currentUser: UserProfile?
    
    enum DeepLinkAction {
        case openProfile
    }
    @Published var deepLinkAction: DeepLinkAction?
    
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
        isLoggedIn = getToken() != nil && userDefaults.string(forKey: userEmailKey) != nil
        if isLoggedIn {
            loadCurrentUser()
        }
    }
    
    func saveToken(_ token: String) {
        userDefaults.set(token, forKey: tokenKey)
        isLoggedIn = true
    }
    
    func getToken() -> String? {
        return userDefaults.string(forKey: tokenKey)
    }
    
    func saveUser(fullName: String, email: String, role: String? = nil) {
        let id = userDefaults.string(forKey: userIdKey) ?? generateNumericId()
        userDefaults.set(id, forKey: userIdKey)
        userDefaults.set(fullName, forKey: userFullNameKey)
        userDefaults.set(email, forKey: userEmailKey)
        if let role = role {
            userDefaults.set(role, forKey: userRoleKey)
        }
        loadCurrentUser()
    }
    
    func getUserRole() -> String? {
        return userDefaults.string(forKey: userRoleKey)
    }
    
    func loadCurrentUser() {
        guard let email = userDefaults.string(forKey: userEmailKey),
              let fullName = userDefaults.string(forKey: userFullNameKey) else {
            currentUser = nil
            return
        }
        
        let id = userDefaults.string(forKey: userIdKey) ?? generateNumericId()
        let role = userDefaults.string(forKey: userRoleKey) ?? "user"
        
        currentUser = UserProfile(
            id: id,
            email: email,
            fullName: fullName,
            role: role,
            phone: nil,
            course: nil,
            speciality: nil,
            status: nil,
            university: nil,
            payment: nil,
            penalties: nil,
            notes: nil,
            studentId: nil,
            emailVerified: true,
            avatarUrl: nil
        )
    }
    
    func logout() {
        userDefaults.removeObject(forKey: tokenKey)
        userDefaults.removeObject(forKey: userEmailKey)
        userDefaults.removeObject(forKey: userFullNameKey)
        userDefaults.removeObject(forKey: userIdKey)
        userDefaults.removeObject(forKey: userRoleKey)
        isLoggedIn = false
        currentUser = nil
    }
    
    func isGuest() -> Bool {
        guard let email = userDefaults.string(forKey: userEmailKey) else { return true }
        return email == "guest@local"
    }
    
    private func generateNumericId(length: Int = 6) -> String {
        let random = Int.random(in: 0..<Int(pow(10.0, Double(length))))
        return String(format: "%0\(length)d", random)
    }
}

