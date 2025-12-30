import Foundation

class ApiClient: ObservableObject {
    static let shared = ApiClient()
    
    // Замените на ваш реальный URL сервера
    let baseURL = "https://api.kleos-study.ru"
    
    private init() {}
    
    // MARK: - Helper Methods
    func getFullUrl(_ relativePath: String?) -> URL? {
        guard let path = relativePath, !path.isEmpty else { return nil }
        if path.hasPrefix("http") {
            return URL(string: path)
        }
        let cleanPath = path.hasPrefix("/") ? String(path.dropFirst()) : path
        return URL(string: "\(baseURL)/\(cleanPath)")
    }
    
    func createRequest(url: URL, method: String = "GET", body: Data? = nil) -> URLRequest {
        var request = URLRequest(url: url)
        request.httpMethod = method
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        
        if let token = SessionManager.shared.getToken() {
            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }
        
        if let body = body {
            request.httpBody = body
        }
        
        return request
    }
    
    // MARK: - News API
    func fetchNews() async throws -> [NewsItem] {
        guard let url = URL(string: "\(baseURL)/api/news") else {
            throw URLError(.badURL)
        }
        
        let request = createRequest(url: url)
        let (data, _) = try await URLSession.shared.data(for: request)
        return try JSONDecoder().decode([NewsItem].self, from: data)
    }
    
    func fetchNewsDetail(id: String) async throws -> NewsItem {
        guard let url = URL(string: "\(baseURL)/api/news/\(id)") else {
            throw URLError(.badURL)
        }
        
        let request = createRequest(url: url)
        let (data, _) = try await URLSession.shared.data(for: request)
        return try JSONDecoder().decode(NewsItem.self, from: data)
    }
    
    // MARK: - Auth API
    func login(email: String, password: String) async throws -> AuthResponse {
        guard let url = URL(string: "\(baseURL)/api/auth/login") else {
            throw URLError(.badURL)
        }
        
        let loginRequest = LoginRequest(email: email, password: password)
        let body = try JSONEncoder().encode(loginRequest)
        let request = createRequest(url: url, method: "POST", body: body)
        
        let (data, response) = try await URLSession.shared.data(for: request)
        
        if let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode != 200 {
            if let errorResponse = try? JSONDecoder().decode(AuthResponse.self, from: data) {
                throw ApiError.serverError(errorResponse.error ?? "Login failed")
            }
            throw ApiError.httpError(httpResponse.statusCode)
        }
        
        return try JSONDecoder().decode(AuthResponse.self, from: data)
    }
    
    func register(fullName: String, email: String, password: String) async throws -> AuthResponse {
        guard let url = URL(string: "\(baseURL)/api/auth/register") else {
            throw URLError(.badURL)
        }
        
        let registerRequest = RegisterRequest(fullName: fullName, email: email, password: password)
        let body = try JSONEncoder().encode(registerRequest)
        let request = createRequest(url: url, method: "POST", body: body)
        
        let (data, response) = try await URLSession.shared.data(for: request)
        
        if let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode != 200 {
            if let errorResponse = try? JSONDecoder().decode(AuthResponse.self, from: data) {
                throw ApiError.serverError(errorResponse.error ?? "Registration failed")
            }
            throw ApiError.httpError(httpResponse.statusCode)
        }
        
        return try JSONDecoder().decode(AuthResponse.self, from: data)
    }
    
    // MARK: - User Profile API
    func getProfile() async throws -> UserProfile {
        guard let url = URL(string: "\(baseURL)/api/users/me") else {
            throw URLError(.badURL)
        }
        
        let request = createRequest(url: url)
        let (data, _) = try await URLSession.shared.data(for: request)
        return try JSONDecoder().decode(UserProfile.self, from: data)
    }
    
    func updateProfile(_ request: UpdateProfileRequest) async throws -> UserProfile {
        guard let url = URL(string: "\(baseURL)/api/users/me") else {
            throw URLError(.badURL)
        }
        
        let body = try JSONEncoder().encode(request)
        let urlRequest = createRequest(url: url, method: "PUT", body: body)
        
        let (data, _) = try await URLSession.shared.data(for: urlRequest)
        return try JSONDecoder().decode(UserProfile.self, from: data)
    }
    
    // MARK: - Universities API
    func fetchUniversities() async throws -> [University] {
        guard let url = URL(string: "\(baseURL)/api/universities") else {
            throw URLError(.badURL)
        }
        
        let request = createRequest(url: url)
        let (data, _) = try await URLSession.shared.data(for: request)
        return try JSONDecoder().decode([University].self, from: data)
    }
    
    func fetchUniversity(id: String) async throws -> University {
        guard let url = URL(string: "\(baseURL)/api/universities/\(id)") else {
            throw URLError(.badURL)
        }
        
        let request = createRequest(url: url)
        let (data, _) = try await URLSession.shared.data(for: request)
        return try JSONDecoder().decode(University.self, from: data)
    }
    
    // MARK: - Programs API
    func fetchPrograms(filters: ProgramFilters? = nil) async throws -> [Program] {
        var urlString = "\(baseURL)/api/programs"
        var queryItems: [String] = []
        
        if let filters = filters {
            if let language = filters.language {
                queryItems.append("language=\(language)")
            }
            if let educationLevel = filters.educationLevel {
                queryItems.append("educationLevel=\(educationLevel)")
            }
            if let universityId = filters.universityId {
                queryItems.append("universityId=\(universityId)")
            }
            if let searchQuery = filters.searchQuery {
                queryItems.append("search=\(searchQuery.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? "")")
            }
        }
        
        if !queryItems.isEmpty {
            urlString += "?" + queryItems.joined(separator: "&")
        }
        
        guard let url = URL(string: urlString) else {
            throw URLError(.badURL)
        }
        
        let request = createRequest(url: url)
        let (data, _) = try await URLSession.shared.data(for: request)
        return try JSONDecoder().decode([Program].self, from: data)
    }
    
    // MARK: - Gallery API
    func fetchGallery() async throws -> [GalleryItem] {
        guard let url = URL(string: "\(baseURL)/api/gallery") else {
            throw URLError(.badURL)
        }
        
        let request = createRequest(url: url)
        let (data, _) = try await URLSession.shared.data(for: request)
        return try JSONDecoder().decode([GalleryItem].self, from: data)
    }
    
    // MARK: - Partners API
    func fetchPartners() async throws -> [Partner] {
        guard let url = URL(string: "\(baseURL)/api/partners") else {
            throw URLError(.badURL)
        }
        
        let request = createRequest(url: url)
        let (data, _) = try await URLSession.shared.data(for: request)
        return try JSONDecoder().decode([Partner].self, from: data)
    }
    
    // MARK: - Admission API
    func submitAdmission(_ application: AdmissionApplication) async throws -> AdmissionResponse {
        guard let url = URL(string: "\(baseURL)/api/admissions") else {
            throw URLError(.badURL)
        }
        
        let body = try JSONEncoder().encode(application)
        let request = createRequest(url: url, method: "POST", body: body)
        
        let (data, _) = try await URLSession.shared.data(for: request)
        return try JSONDecoder().decode(AdmissionResponse.self, from: data)
    }
    
    // MARK: - Chat API
    func fetchMessages() async throws -> [ChatMessage] {
        guard let url = URL(string: "\(baseURL)/api/chats/messages") else {
            throw URLError(.badURL)
        }
        
        let request = createRequest(url: url)
        let (data, _) = try await URLSession.shared.data(for: request)
        return try JSONDecoder().decode([ChatMessage].self, from: data)
    }
    
    func sendMessage(text: String) async throws -> ChatMessage {
        guard let url = URL(string: "\(baseURL)/api/chats/messages") else {
            throw URLError(.badURL)
        }
        
        let body = try JSONEncoder().encode(["text": text])
        let request = createRequest(url: url, method: "POST", body: body)
        
        let (data, _) = try await URLSession.shared.data(for: request)
        return try JSONDecoder().decode(ChatMessage.self, from: data)
    }
    
    // MARK: - Settings API
    func fetchConsentText(language: String) async throws -> ConsentText {
        guard let url = URL(string: "\(baseURL)/api/settings/consent/\(language)") else {
            throw URLError(.badURL)
        }
        
        let request = createRequest(url: url)
        let (data, _) = try await URLSession.shared.data(for: request)
        return try JSONDecoder().decode(ConsentText.self, from: data)
    }
    
    func fetchCountries() async throws -> [Country] {
        guard let url = URL(string: "\(baseURL)/api/settings/countries") else {
            throw URLError(.badURL)
        }
        
        let request = createRequest(url: url)
        let (data, _) = try await URLSession.shared.data(for: request)
        return try JSONDecoder().decode([Country].self, from: data)
    }
}

enum ApiError: Error {
    case badURL
    case httpError(Int)
    case serverError(String)
    case decodingError
    case networkError(Error)
}
