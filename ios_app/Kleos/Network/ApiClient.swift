import Foundation
import Combine

class ApiClient: ObservableObject {
    static let shared = ApiClient()
    @Published var lastError: String? = nil
    
    let baseURL = "https://api.kleos-study.ru"
    private let apiPrefix = "/api"
    
    private lazy var urlSession: URLSession = {
        let config = URLSessionConfiguration.default
        config.timeoutIntervalForRequest = 60.0
        return URLSession(configuration: config)
    }()
    
    private init() {}
    
    func getFullUrl(_ path: String?) -> URL? {
        guard let p = path, !p.isEmpty else { return nil }
        if p.hasPrefix("http") { return URL(string: p) }
        return URL(string: "\(baseURL)/\(p.hasPrefix("/") ? String(p.dropFirst()) : p)")
    }
    
    func createRequest(url: URL, method: String = "GET", body: Data? = nil) -> URLRequest {
        var req = URLRequest(url: url)
        req.httpMethod = method
        req.setValue("application/json", forHTTPHeaderField: "Content-Type")
        let lang = UserDefaults.standard.string(forKey: "selectedLanguage") ?? "en"
        req.setValue(lang, forHTTPHeaderField: "Accept-Language")
        
        if let token = SessionManager.shared.getToken(), token.contains(".") {
            req.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }
        if let b = body { req.httpBody = b }
        return req
    }
    
    private func performRequest(_ request: URLRequest) async throws -> (Data, HTTPURLResponse) {
        let (data, response) = try await urlSession.data(for: request)
        guard let httpResponse = response as? HTTPURLResponse else { throw ApiError.badURL }
        
        // ЕСЛИ 401 - проверяем, не устарел ли токен
        if httpResponse.statusCode == 401 {
            let currentToken = SessionManager.shared.getToken() ?? ""
            let requestToken = request.value(forHTTPHeaderField: "Authorization")?.replacingOccurrences(of: "Bearer ", with: "") ?? ""
            
            // Логаут только если токен в запросе совпадает с текущим (значит он реально плохой)
            if !currentToken.isEmpty && currentToken == requestToken {
                print("🛑 Token expired. Logging out...")
                await MainActor.run { SessionManager.shared.logout() }
            }
            throw ApiError.unauthorized
        }
        return (data, httpResponse)
    }
    
    func fetchNews() async throws -> [NewsItem] {
        guard let url = URL(string: "\(baseURL)\(apiPrefix)/news") else { throw ApiError.badURL }
        let (data, _) = try await performRequest(createRequest(url: url))
        let decoder = JSONDecoder(); decoder.dateDecodingStrategy = .iso8601
        return try decoder.decode([NewsItem].self, from: data)
    }
    
    func login(email: String, password: String) async throws -> AuthResponse {
        guard let url = URL(string: "\(baseURL)\(apiPrefix)/auth/login") else { throw ApiError.badURL }
        let body = try JSONEncoder().encode(LoginRequest(email: email, password: password))
        let (data, _) = try await performRequest(createRequest(url: url, method: "POST", body: body))
        return try JSONDecoder().decode(AuthResponse.self, from: data)
    }
    
    func register(fullName: String, email: String, password: String) async throws -> AuthResponse {
        guard let url = URL(string: "\(baseURL)\(apiPrefix)/auth/register") else { throw ApiError.badURL }
        let body = try JSONEncoder().encode(RegisterRequest(fullName: fullName, email: email, password: password))
        let (data, _) = try await performRequest(createRequest(url: url, method: "POST", body: body))
        return try JSONDecoder().decode(AuthResponse.self, from: data)
    }
    
    func verifyConsume(token: String) async throws -> AuthResponse {
        guard let url = URL(string: "\(baseURL)\(apiPrefix)/auth/verify/consume") else { throw ApiError.badURL }
        let body = try JSONEncoder().encode(["token": token])
        let (data, _) = try await performRequest(createRequest(url: url, method: "POST", body: body))
        return try JSONDecoder().decode(AuthResponse.self, from: data)
    }
    
    func getProfile() async throws -> UserProfile {
        guard let url = URL(string: "\(baseURL)\(apiPrefix)/users/me") else { throw ApiError.badURL }
        guard let t = SessionManager.shared.getToken(), t.contains(".") else { throw ApiError.unauthorized }
        let (data, _) = try await performRequest(createRequest(url: url))
        return try JSONDecoder().decode(UserProfile.self, from: data)
    }
    
    func fetchUniversities() async throws -> [University] {
        guard let url = URL(string: "\(baseURL)\(apiPrefix)/universities") else { throw ApiError.badURL }
        let (data, _) = try await performRequest(createRequest(url: url))
        return try JSONDecoder().decode([University].self, from: data)
    }
    
    func fetchPrograms(filters: ProgramFilters? = nil) async throws -> [Program] {
        var urlString = "\(baseURL)\(apiPrefix)/programs"
        if let f = filters {
            var q: [String] = []
            if let l = f.language { q.append("language=\(l)") }
            if let lv = f.level { q.append("level=\(lv)") }
            if let u = f.university { q.append("university=\(u)") }
            if let ui = f.universityId { q.append("universityId=\(ui)") }
            if let search = f.searchQuery { q.append("q=\(search.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? "")") }
            if !q.isEmpty { urlString += "?" + q.joined(separator: "&") }
        }
        guard let url = URL(string: urlString) else { throw ApiError.badURL }
        let (data, _) = try await performRequest(createRequest(url: url))
        return try JSONDecoder().decode([Program].self, from: data)
    }
    
    func fetchGallery() async throws -> [GalleryItem] {
        guard let url = URL(string: "\(baseURL)\(apiPrefix)/gallery") else { throw ApiError.badURL }
        let (data, _) = try await performRequest(createRequest(url: url))
        return try JSONDecoder().decode([GalleryItem].self, from: data)
    }
    
    func fetchPartners() async throws -> [Partner] {
        guard let url = URL(string: "\(baseURL)\(apiPrefix)/partners") else { throw ApiError.badURL }
        let (data, _) = try await performRequest(createRequest(url: url))
        return try JSONDecoder().decode([Partner].self, from: data)
    }
}

enum ApiError: Error {
    case badURL, unauthorized, serverError(String)
}
