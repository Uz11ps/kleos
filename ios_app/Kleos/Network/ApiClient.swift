import Foundation

enum ApiError: Error {
    case badURL
    case networkError(Error)
    case serverError(String)
    case decodingError
    case unauthorized
}

class ApiClient: ObservableObject {
    static let shared = ApiClient()
    
    // Используйте ваш реальный URL
    let baseURL = "https://api.kleos-study.ru"
    let apiPrefix = "/api"
    
    private let urlSession: URLSession
    
    private init() {
        let config = URLSessionConfiguration.default
        config.timeoutIntervalForRequest = 30
        self.urlSession = URLSession(configuration: config)
    }
    
    func createRequest(url: URL, method: String = "GET", body: Data? = nil) -> URLRequest {
        var req = URLRequest(url: url)
        req.httpMethod = method
        req.setValue("application/json", forHTTPHeaderField: "Content-Type")
        
        // Устанавливаем язык из настроек или по умолчанию
        let lang = UserDefaults.standard.string(forKey: "selectedLanguage") ?? "en"
        req.setValue(lang, forHTTPHeaderField: "Accept-Language")
        
        // Добавляем токен, ТОЛЬКО если это настоящий JWT
        if let token = SessionManager.shared.getToken(), token.contains(".") {
            req.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }
        
        if let b = body {
            req.httpBody = b
        }
        return req
    }
    
    private func performRequest(_ request: URLRequest) async throws -> (Data, HTTPURLResponse) {
        let (data, response) = try await urlSession.data(for: request)
        guard let httpResponse = response as? HTTPURLResponse else {
            throw ApiError.badURL
        }
        
        // Если 401 - проверяем, не устарел ли токен
        if httpResponse.statusCode == 401 {
            let currentToken = SessionManager.shared.getToken() ?? ""
            let requestToken = request.value(forHTTPHeaderField: "Authorization")?.replacingOccurrences(of: "Bearer ", with: "") ?? ""
            
            // ЛОГАУТ только если токен в запросе совпадает с текущим (значит он реально плохой)
            // И ТОЛЬКО если это не гость
            if !currentToken.isEmpty && currentToken.contains(".") && currentToken == requestToken {
                print("🛑 Real token expired or invalid. Logging out...")
                await MainActor.run {
                    SessionManager.shared.logout()
                }
            }
            throw ApiError.unauthorized
        }
        
        if !(200...299).contains(httpResponse.statusCode) {
            let errorMessage = try? JSONDecoder().decode(ApiResponse<String>.self, from: data).error
            throw ApiError.serverError(errorMessage ?? "Server error: \(httpResponse.statusCode)")
        }
        
        return (data, httpResponse)
    }
    
    // Auth
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
    
    // Profile
    func getProfile() async throws -> UserProfile {
        // ЗАЩИТА: Не делаем запрос, если мы гость (нет точки в токене)
        guard let t = SessionManager.shared.getToken(), t.contains(".") else {
            throw ApiError.unauthorized
        }
        
        guard let url = URL(string: "\(baseURL)\(apiPrefix)/users/me") else { throw ApiError.badURL }
        let request = createRequest(url: url)
        let (data, _) = try await performRequest(request)
        let response = try JSONDecoder().decode(ApiResponse<UserProfile>.self, from: data)
        if let user = response.data { return user }
        throw ApiError.decodingError
    }
    
    // News
    func getNews() async throws -> [NewsItem] {
        guard let url = URL(string: "\(baseURL)\(apiPrefix)/news") else { throw ApiError.badURL }
        let request = createRequest(url: url)
        let (data, _) = try await performRequest(request)
        return try JSONDecoder().decode([NewsItem].self, from: data)
    }
    
    // Universities
    func getUniversities() async throws -> [University] {
        guard let url = URL(string: "\(baseURL)\(apiPrefix)/universities") else { throw ApiError.badURL }
        let request = createRequest(url: url)
        let (data, _) = try await performRequest(request)
        return try JSONDecoder().decode([University].self, from: data)
    }
    
    func getUniversity(id: String) async throws -> University {
        guard let url = URL(string: "\(baseURL)\(apiPrefix)/universities/\(id)") else { throw ApiError.badURL }
        let request = createRequest(url: url)
        let (data, _) = try await performRequest(request)
        let response = try JSONDecoder().decode(ApiResponse<University>.self, from: data)
        if let university = response.data { return university }
        throw ApiError.decodingError
    }
    
    // Gallery
    func getGallery() async throws -> [GalleryItem] {
        guard let url = URL(string: "\(baseURL)\(apiPrefix)/gallery") else { throw ApiError.badURL }
        let request = createRequest(url: url)
        let (data, _) = try await performRequest(request)
        return try JSONDecoder().decode([GalleryItem].self, from: data)
    }
    
    // Partners
    func getPartners() async throws -> [Partner] {
        guard let url = URL(string: "\(baseURL)\(apiPrefix)/partners") else { throw ApiError.badURL }
        let request = createRequest(url: url)
        let (data, _) = try await performRequest(request)
        return try JSONDecoder().decode([Partner].self, from: data)
    }
    
    // Programs
    func getPrograms(language: String? = nil, level: String? = nil, universityId: String? = nil) async throws -> [Program] {
        var components = URLComponents(string: "\(baseURL)\(apiPrefix)/programs")!
        var queryItems: [URLQueryItem] = []
        if let lang = language { queryItems.append(URLQueryItem(name: "language", value: lang)) }
        if let lvl = level { queryItems.append(URLQueryItem(name: "level", value: lvl)) }
        if let uniId = universityId { queryItems.append(URLQueryItem(name: "universityId", value: uniId)) }
        components.queryItems = queryItems
        
        let request = createRequest(url: components.url!)
        let (data, _) = try await performRequest(request)
        return try JSONDecoder().decode([Program].self, from: data)
    }

    func getFullUrl(_ path: String?) -> URL? {
        guard let raw = path, !raw.isEmpty else { return nil }
        if raw.hasPrefix("http://") || raw.hasPrefix("https://") {
            return URL(string: raw)
        }
        let normalized = raw.hasPrefix("/") ? raw : "/\(raw)"
        return URL(string: "\(baseURL)\(normalized)")
    }

    // MARK: - Legacy wrappers used by current iOS views
    func fetchNews() async throws -> [NewsItem] {
        try await getNews()
    }

    func fetchNewsDetail(id: String) async throws -> NewsItem {
        guard let url = URL(string: "\(baseURL)\(apiPrefix)/news/\(id)") else { throw ApiError.badURL }
        let request = createRequest(url: url)
        let (data, _) = try await performRequest(request)
        return try JSONDecoder().decode(NewsItem.self, from: data)
    }

    func fetchUniversities() async throws -> [University] {
        try await getUniversities()
    }

    func fetchPrograms(filters: ProgramFilters) async throws -> [Program] {
        var components = URLComponents(string: "\(baseURL)\(apiPrefix)/programs")!
        var queryItems: [URLQueryItem] = []
        if let q = filters.searchQuery, !q.isEmpty { queryItems.append(URLQueryItem(name: "q", value: q)) }
        if let lang = filters.language, !lang.isEmpty { queryItems.append(URLQueryItem(name: "language", value: lang)) }
        if let lvl = filters.level, !lvl.isEmpty { queryItems.append(URLQueryItem(name: "level", value: lvl)) }
        if let uni = filters.university, !uni.isEmpty { queryItems.append(URLQueryItem(name: "university", value: uni)) }
        if let uniId = filters.universityId, !uniId.isEmpty { queryItems.append(URLQueryItem(name: "universityId", value: uniId)) }
        components.queryItems = queryItems.isEmpty ? nil : queryItems

        guard let url = components.url else { throw ApiError.badURL }
        let request = createRequest(url: url)
        let (data, _) = try await performRequest(request)
        return try JSONDecoder().decode([Program].self, from: data)
    }

    func fetchProgramDetail(id: String) async throws -> Program {
        guard let url = URL(string: "\(baseURL)\(apiPrefix)/programs/\(id)") else { throw ApiError.badURL }
        let request = createRequest(url: url)
        let (data, _) = try await performRequest(request)
        return try JSONDecoder().decode(Program.self, from: data)
    }

    func fetchGallery() async throws -> [GalleryItem] {
        try await getGallery()
    }

    func fetchPartners() async throws -> [Partner] {
        try await getPartners()
    }

    // MARK: - Chats
    func ensureChat() async throws -> String {
        guard let url = URL(string: "\(baseURL)\(apiPrefix)/chats") else { throw ApiError.badURL }
        let request = createRequest(url: url, method: "POST", body: try JSONSerialization.data(withJSONObject: [:]))
        let (data, _) = try await performRequest(request)
        let decoded = try JSONDecoder().decode(ChatCreateResponse.self, from: data)
        return decoded.id
    }

    func fetchMessages() async throws -> [ChatMessage] {
        let chatId = try await ensureChat()
        guard let url = URL(string: "\(baseURL)\(apiPrefix)/chats/\(chatId)/messages") else { throw ApiError.badURL }
        let request = createRequest(url: url)
        let (data, _) = try await performRequest(request)
        return try JSONDecoder().decode([ChatMessage].self, from: data)
    }

    func sendMessage(text: String) async throws {
        let chatId = try await ensureChat()
        guard let url = URL(string: "\(baseURL)\(apiPrefix)/chats/\(chatId)/messages") else { throw ApiError.badURL }
        let body = try JSONSerialization.data(withJSONObject: ["text": text])
        let request = createRequest(url: url, method: "POST", body: body)
        _ = try await performRequest(request)
    }
}
