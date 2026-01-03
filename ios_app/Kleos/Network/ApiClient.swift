import Foundation
import Combine

class ApiClient: ObservableObject {
    static let shared = ApiClient()
    
    @Published var lastError: String? = nil
    
    let baseURL = "https://api.kleos-study.ru"
    private let apiPrefix = "/api"
    
    private lazy var urlSession: URLSession = {
        let configuration = URLSessionConfiguration.default
        configuration.timeoutIntervalForRequest = 120.0
        configuration.timeoutIntervalForResource = 120.0
        return URLSession(configuration: configuration)
    }()
    
    private init() {}
    
    // MARK: - Helper Methods
    func getFullUrl(_ relativePath: String?) -> URL? {
        guard let path = relativePath, !path.isEmpty else { return nil }
        if path.hasPrefix("http") { return URL(string: path) }
        let cleanPath = path.hasPrefix("/") ? String(path.dropFirst()) : path
        return URL(string: "\(baseURL)/\(cleanPath)")
    }
    
    func createRequest(url: URL, method: String = "GET", body: Data? = nil) -> URLRequest {
        var request = URLRequest(url: url)
        request.httpMethod = method
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        
        let lang = UserDefaults.standard.string(forKey: "selectedLanguage") ?? "en"
        request.setValue(lang, forHTTPHeaderField: "Accept-Language")
        
        // Добавляем JWT токен ТОЛЬКО если он настоящий (с точками)
        if let token = SessionManager.shared.getToken(), token.contains(".") {
            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }
        
        if let body = body {
            request.httpBody = body
        }
        
        return request
    }
    
    // Вспомогательный метод для выполнения запроса с автоматической очисткой 401
    private func performRequest(_ request: URLRequest) async throws -> (Data, HTTPURLResponse) {
        let (data, response) = try await urlSession.data(for: request)
        
        guard let httpResponse = response as? HTTPURLResponse else {
            throw ApiError.badURL
        }
        
        // ЕСЛИ 401 - ПРИНУДИТЕЛЬНО ВЫХОДИМ ИЗ АККАУНТА (останавливает 401 спам)
        if httpResponse.statusCode == 401 {
            print("🛑 HTTP 401 Detected. Forcing logout...")
            await MainActor.run {
                SessionManager.shared.logout()
            }
            throw ApiError.unauthorized
        }
        
        return (data, httpResponse)
    }
    
    // MARK: - News API
    func fetchNews() async throws -> [NewsItem] {
        guard let url = URL(string: "\(baseURL)\(apiPrefix)/news") else { throw ApiError.badURL }
        let request = createRequest(url: url)
        let (data, _) = try await performRequest(request)
        
        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .iso8601
        return try decoder.decode([NewsItem].self, from: data)
    }
    
    func fetchNewsDetail(id: String) async throws -> NewsItem {
        guard let url = URL(string: "\(baseURL)\(apiPrefix)/news/\(id)") else { throw ApiError.badURL }
        let request = createRequest(url: url)
        let (data, _) = try await performRequest(request)
        return try JSONDecoder().decode(NewsItem.self, from: data)
    }
    
    // MARK: - Auth API
    func login(email: String, password: String) async throws -> AuthResponse {
        guard let url = URL(string: "\(baseURL)\(apiPrefix)/auth/login") else { throw ApiError.badURL }
        let body = try JSONEncoder().encode(LoginRequest(email: email, password: password))
        let request = createRequest(url: url, method: "POST", body: body)
        let (data, _) = try await performRequest(request)
        return try JSONDecoder().decode(AuthResponse.self, from: data)
    }
    
    func register(fullName: String, email: String, password: String) async throws -> AuthResponse {
        guard let url = URL(string: "\(baseURL)\(apiPrefix)/auth/register") else { throw ApiError.badURL }
        let body = try JSONEncoder().encode(RegisterRequest(fullName: fullName, email: email, password: password))
        let request = createRequest(url: url, method: "POST", body: body)
        let (data, _) = try await performRequest(request)
        return try JSONDecoder().decode(AuthResponse.self, from: data)
    }
    
    func verifyConsume(token: String) async throws -> AuthResponse {
        guard let url = URL(string: "\(baseURL)\(apiPrefix)/auth/verify/consume") else { throw ApiError.badURL }
        let body = try JSONEncoder().encode(["token": token])
        let request = createRequest(url: url, method: "POST", body: body)
        let (data, _) = try await performRequest(request)
        return try JSONDecoder().decode(AuthResponse.self, from: data)
    }
    
    // MARK: - User Profile API
    func getProfile() async throws -> UserProfile {
        guard let url = URL(string: "\(baseURL)\(apiPrefix)/users/me") else { throw ApiError.badURL }
        
        // Дополнительная защита: не шлем запрос без JWT
        guard let token = SessionManager.shared.getToken(), token.contains(".") else {
            throw ApiError.unauthorized
        }
        
        let request = createRequest(url: url)
        let (data, _) = try await performRequest(request)
        return try JSONDecoder().decode(UserProfile.self, from: data)
    }
    
    func updateProfile(_ request: UpdateProfileRequest) async throws -> UserProfile {
        guard let url = URL(string: "\(baseURL)\(apiPrefix)/users/me") else { throw ApiError.badURL }
        let body = try JSONEncoder().encode(request)
        let urlRequest = createRequest(url: url, method: "PUT", body: body)
        let (data, _) = try await performRequest(urlRequest)
        return try JSONDecoder().decode(UserProfile.self, from: data)
    }
    
    // MARK: - Universities API
    func fetchUniversities() async throws -> [University] {
        guard let url = URL(string: "\(baseURL)\(apiPrefix)/universities") else { throw ApiError.badURL }
        let request = createRequest(url: url)
        let (data, _) = try await performRequest(request)
        return try JSONDecoder().decode([University].self, from: data)
    }
    
    func fetchUniversity(id: String) async throws -> University {
        guard let url = URL(string: "\(baseURL)\(apiPrefix)/universities/\(id)") else { throw ApiError.badURL }
        let request = createRequest(url: url)
        let (data, _) = try await performRequest(request)
        return try JSONDecoder().decode(University.self, from: data)
    }
    
    // MARK: - Programs API
    func fetchPrograms(filters: ProgramFilters? = nil) async throws -> [Program] {
        var urlString = "\(baseURL)\(apiPrefix)/programs"
        var queryItems: [String] = []
        if let f = filters {
            if let l = f.language { queryItems.append("language=\(l)") }
            if let lv = f.level { queryItems.append("level=\(lv)") }
            if let u = f.university { queryItems.append("university=\(u)") }
            if let ui = f.universityId { queryItems.append("universityId=\(ui)") }
            if let q = f.searchQuery { queryItems.append("q=\(q.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? "")") }
        }
        if !queryItems.isEmpty { urlString += "?" + queryItems.joined(separator: "&") }
        guard let url = URL(string: urlString) else { throw ApiError.badURL }
        let request = createRequest(url: url)
        let (data, _) = try await performRequest(request)
        return try JSONDecoder().decode([Program].self, from: data)
    }
    
    // MARK: - Gallery API
    func fetchGallery() async throws -> [GalleryItem] {
        guard let url = URL(string: "\(baseURL)\(apiPrefix)/gallery") else { throw ApiError.badURL }
        let request = createRequest(url: url)
        let (data, _) = try await performRequest(request)
        return try JSONDecoder().decode([GalleryItem].self, from: data)
    }
    
    // MARK: - Partners API
    func fetchPartners() async throws -> [Partner] {
        guard let url = URL(string: "\(baseURL)\(apiPrefix)/partners") else { throw ApiError.badURL }
        let request = createRequest(url: url)
        let (data, _) = try await performRequest(request)
        return try JSONDecoder().decode([Partner].self, from: data)
    }
}

enum ApiError: Error {
    case badURL
    case httpError(Int)
    case unauthorized
    case serverError(String)
}
