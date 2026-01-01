import Foundation

class ApiClient: ObservableObject {
    static let shared = ApiClient()
    
    // Замените на ваш реальный URL сервера
    let baseURL = "https://api.kleos-study.ru"
    
    // Используем относительные пути без /api префикса, так как сервер работает без него
    private let apiPrefix = ""
    
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
        let urlString = "\(baseURL)\(apiPrefix)/news"
        print("🌐 Fetching news from: \(urlString)")
        
        guard let url = URL(string: urlString) else {
            print("❌ Invalid URL: \(urlString)")
            throw URLError(.badURL)
        }
        
        let request = createRequest(url: url)
        print("📤 Request URL: \(url.absoluteString)")
        print("📤 Request method: \(request.httpMethod ?? "GET")")
        print("📤 Request headers: \(request.allHTTPHeaderFields ?? [:])")
        
        do {
            let (data, response) = try await URLSession.shared.data(for: request)
        
            if let httpResponse = response as? HTTPURLResponse {
                let responseString = String(data: data, encoding: .utf8) ?? "no data"
                print("🔍 News response (\(httpResponse.statusCode)), size: \(data.count) bytes")
                print("🔍 Response headers: \(httpResponse.allHeaderFields)")
                print("🔍 Response body (first 500 chars): \(responseString.prefix(500))")
                
                if httpResponse.statusCode != 200 {
                    print("❌ HTTP Error: \(httpResponse.statusCode)")
                    throw ApiError.httpError(httpResponse.statusCode)
                }
            }
            
            do {
            let decoder = JSONDecoder()
            // Настраиваем декодер для правильной обработки дат
            decoder.dateDecodingStrategy = .iso8601
            let items = try decoder.decode([NewsItem].self, from: data)
            print("✅ Successfully decoded \(items.count) news items")
            if items.isEmpty {
                print("⚠️ Warning: News array is empty")
            }
            return items
        } catch let decodingError {
            print("❌ Decode error: \(decodingError)")
            let responseString = String(data: data, encoding: .utf8) ?? "no data"
            print("📦 Raw JSON (first 1000 chars): \(responseString.prefix(1000))")
            
            // Пробуем декодировать как массив словарей для диагностики
            if let jsonArray = try? JSONSerialization.jsonObject(with: data) as? [[String: Any]] {
                print("📊 JSON structure: Array with \(jsonArray.count) items")
                if let firstItem = jsonArray.first {
                    print("📋 First item keys: \(firstItem.keys.joined(separator: ", "))")
                }
            }
            
            throw decodingError
        }
    }
    
    func fetchNewsDetail(id: String) async throws -> NewsItem {
        guard let url = URL(string: "\(baseURL)\(apiPrefix)/news/\(id)") else {
            throw URLError(.badURL)
        }
        
        let request = createRequest(url: url)
        let (data, _) = try await URLSession.shared.data(for: request)
        return try JSONDecoder().decode(NewsItem.self, from: data)
    }
    
    // MARK: - Auth API
    func login(email: String, password: String) async throws -> AuthResponse {
        guard let url = URL(string: "\(baseURL)\(apiPrefix)/auth/login") else {
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
        guard let url = URL(string: "\(baseURL)\(apiPrefix)/auth/register") else {
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
    
    func verifyConsume(token: String) async throws -> AuthResponse {
        guard let url = URL(string: "\(baseURL)\(apiPrefix)/auth/verify/consume") else {
            throw URLError(.badURL)
        }
        
        let body = try JSONEncoder().encode(["token": token])
        let request = createRequest(url: url, method: "POST", body: body)
        
        let (data, response) = try await URLSession.shared.data(for: request)
        
        if let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode != 200 {
            if let errorResponse = try? JSONDecoder().decode(AuthResponse.self, from: data) {
                throw ApiError.serverError(errorResponse.error ?? "Verification failed")
            }
            throw ApiError.httpError(httpResponse.statusCode)
        }
        
        return try JSONDecoder().decode(AuthResponse.self, from: data)
    }
    
    func resendVerify(email: String) async throws -> [String: Any] {
        guard let url = URL(string: "\(baseURL)\(apiPrefix)/auth/verify/resend") else {
            throw URLError(.badURL)
        }
        
        let body = try JSONEncoder().encode(["email": email])
        let request = createRequest(url: url, method: "POST", body: body)
        
        let (data, response) = try await URLSession.shared.data(for: request)
        
        if let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode != 200 {
            throw ApiError.httpError(httpResponse.statusCode)
        }
        
        return try JSONSerialization.jsonObject(with: data) as? [String: Any] ?? [:]
    }
    
    // MARK: - User Profile API
    func getProfile() async throws -> UserProfile {
        guard let url = URL(string: "\(baseURL)\(apiPrefix)/users/me") else {
            throw URLError(.badURL)
        }
        
        let request = createRequest(url: url)
        let (data, _) = try await URLSession.shared.data(for: request)
        return try JSONDecoder().decode(UserProfile.self, from: data)
    }
    
    func updateProfile(_ request: UpdateProfileRequest) async throws -> UserProfile {
        guard let url = URL(string: "\(baseURL)\(apiPrefix)/users/me") else {
            throw URLError(.badURL)
        }
        
        let body = try JSONEncoder().encode(request)
        let urlRequest = createRequest(url: url, method: "PUT", body: body)
        
        let (data, _) = try await URLSession.shared.data(for: urlRequest)
        return try JSONDecoder().decode(UserProfile.self, from: data)
    }
    
    func saveFcmToken(_ token: String) async throws {
        guard let url = URL(string: "\(baseURL)\(apiPrefix)/users/fcm-token") else {
            throw URLError(.badURL)
        }
        
        let body = try JSONEncoder().encode(["token": token])
        let request = createRequest(url: url, method: "POST", body: body)
        
        let (_, _) = try await URLSession.shared.data(for: request)
    }
    
    // MARK: - Universities API
    func fetchUniversities() async throws -> [University] {
        guard let url = URL(string: "\(baseURL)\(apiPrefix)/universities") else {
            throw URLError(.badURL)
        }
        
        let request = createRequest(url: url)
        let (data, response) = try await URLSession.shared.data(for: request)
        
        if let httpResponse = response as? HTTPURLResponse {
            print("🔍 Universities response (\(httpResponse.statusCode)): \(String(data: data, encoding: .utf8)?.prefix(500) ?? "no data")")
            
            if httpResponse.statusCode != 200 {
                throw ApiError.httpError(httpResponse.statusCode)
            }
        }
        
        do {
            let decoder = JSONDecoder()
            let items = try decoder.decode([University].self, from: data)
            print("✅ Successfully decoded \(items.count) universities")
            if items.isEmpty {
                print("⚠️ Warning: Universities array is empty")
            }
            return items
        } catch let decodingError {
            print("❌ Decode error: \(decodingError)")
            let responseString = String(data: data, encoding: .utf8) ?? "no data"
            print("📦 Raw JSON (first 1000 chars): \(responseString.prefix(1000))")
            
            // Пробуем декодировать как массив словарей для диагностики
            if let jsonArray = try? JSONSerialization.jsonObject(with: data) as? [[String: Any]] {
                print("📊 JSON structure: Array with \(jsonArray.count) items")
                if let firstItem = jsonArray.first {
                    print("📋 First item keys: \(firstItem.keys.joined(separator: ", "))")
                }
            }
            
            throw decodingError
        }
    }
    
    func fetchUniversity(id: String) async throws -> University {
        guard let url = URL(string: "\(baseURL)\(apiPrefix)/universities/\(id)") else {
            throw URLError(.badURL)
        }
        
        let request = createRequest(url: url)
        let (data, _) = try await URLSession.shared.data(for: request)
        return try JSONDecoder().decode(University.self, from: data)
    }
    
    // MARK: - Programs API
    func fetchPrograms(filters: ProgramFilters? = nil) async throws -> [Program] {
        var urlString = "\(baseURL)\(apiPrefix)/programs"
        var queryItems: [String] = []
        
        if let filters = filters {
            if let language = filters.language {
                queryItems.append("language=\(language)")
            }
            if let level = filters.level {
                queryItems.append("level=\(level)")
            }
            if let university = filters.university {
                queryItems.append("university=\(university)")
            }
            if let universityId = filters.universityId {
                queryItems.append("universityId=\(universityId)")
            }
            if let searchQuery = filters.searchQuery {
                queryItems.append("q=\(searchQuery.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? "")")
            }
        }
        
        if !queryItems.isEmpty {
            urlString += "?" + queryItems.joined(separator: "&")
        }
        
        guard let url = URL(string: urlString) else {
            throw URLError(.badURL)
        }
        
        let request = createRequest(url: url)
        let (data, response) = try await URLSession.shared.data(for: request)
        
        if let httpResponse = response as? HTTPURLResponse {
            print("🔍 Programs response (\(httpResponse.statusCode)): \(String(data: data, encoding: .utf8)?.prefix(500) ?? "no data")")
            
            if httpResponse.statusCode != 200 {
                throw ApiError.httpError(httpResponse.statusCode)
            }
        }
        
        do {
            let decoder = JSONDecoder()
            let items = try decoder.decode([Program].self, from: data)
            print("✅ Successfully decoded \(items.count) programs")
            if items.isEmpty {
                print("⚠️ Warning: Programs array is empty")
            }
            return items
        } catch let decodingError {
            print("❌ Decode error: \(decodingError)")
            let responseString = String(data: data, encoding: .utf8) ?? "no data"
            print("📦 Raw JSON (first 1000 chars): \(responseString.prefix(1000))")
            
            // Пробуем декодировать как массив словарей для диагностики
            if let jsonArray = try? JSONSerialization.jsonObject(with: data) as? [[String: Any]] {
                print("📊 JSON structure: Array with \(jsonArray.count) items")
                if let firstItem = jsonArray.first {
                    print("📋 First item keys: \(firstItem.keys.joined(separator: ", "))")
                }
            }
            
            throw decodingError
        }
    }
    
    func fetchProgram(id: String) async throws -> Program {
        guard let url = URL(string: "\(baseURL)\(apiPrefix)/programs/\(id)") else {
            throw URLError(.badURL)
        }
        
        let request = createRequest(url: url)
        let (data, _) = try await URLSession.shared.data(for: request)
        return try JSONDecoder().decode(Program.self, from: data)
    }
    
    // MARK: - Gallery API
    func fetchGallery() async throws -> [GalleryItem] {
        guard let url = URL(string: "\(baseURL)\(apiPrefix)/gallery") else {
            throw URLError(.badURL)
        }
        
        let request = createRequest(url: url)
        let (data, response) = try await URLSession.shared.data(for: request)
        
        if let httpResponse = response as? HTTPURLResponse {
            print("🔍 Gallery response (\(httpResponse.statusCode)): \(String(data: data, encoding: .utf8)?.prefix(500) ?? "no data")")
            
            if httpResponse.statusCode != 200 {
                throw ApiError.httpError(httpResponse.statusCode)
            }
        }
        
        do {
            let decoder = JSONDecoder()
            let items = try decoder.decode([GalleryItem].self, from: data)
            print("✅ Successfully decoded \(items.count) gallery items")
            if items.isEmpty {
                print("⚠️ Warning: Gallery array is empty")
            }
            return items
        } catch let decodingError {
            print("❌ Decode error: \(decodingError)")
            let responseString = String(data: data, encoding: .utf8) ?? "no data"
            print("📦 Raw JSON (first 1000 chars): \(responseString.prefix(1000))")
            
            // Пробуем декодировать как массив словарей для диагностики
            if let jsonArray = try? JSONSerialization.jsonObject(with: data) as? [[String: Any]] {
                print("📊 JSON structure: Array with \(jsonArray.count) items")
                if let firstItem = jsonArray.first {
                    print("📋 First item keys: \(firstItem.keys.joined(separator: ", "))")
                }
            }
            
            throw decodingError
        }
    }
    
    // MARK: - Partners API
    func fetchPartners() async throws -> [Partner] {
        guard let url = URL(string: "\(baseURL)\(apiPrefix)/partners") else {
            throw URLError(.badURL)
        }
        
        let request = createRequest(url: url)
        let (data, response) = try await URLSession.shared.data(for: request)
        
        if let httpResponse = response as? HTTPURLResponse {
            print("🔍 Partners response (\(httpResponse.statusCode)): \(String(data: data, encoding: .utf8)?.prefix(500) ?? "no data")")
            
            if httpResponse.statusCode != 200 {
                throw ApiError.httpError(httpResponse.statusCode)
            }
        }
        
        do {
            let decoder = JSONDecoder()
            let items = try decoder.decode([Partner].self, from: data)
            print("✅ Successfully decoded \(items.count) partners")
            if items.isEmpty {
                print("⚠️ Warning: Partners array is empty")
            }
            return items
        } catch let decodingError {
            print("❌ Decode error: \(decodingError)")
            let responseString = String(data: data, encoding: .utf8) ?? "no data"
            print("📦 Raw JSON (first 1000 chars): \(responseString.prefix(1000))")
            
            // Пробуем декодировать как массив словарей для диагностики
            if let jsonArray = try? JSONSerialization.jsonObject(with: data) as? [[String: Any]] {
                print("📊 JSON structure: Array with \(jsonArray.count) items")
                if let firstItem = jsonArray.first {
                    print("📋 First item keys: \(firstItem.keys.joined(separator: ", "))")
                }
            }
            
            throw decodingError
        }
    }
    
    // MARK: - Admission API
    func submitAdmission(_ application: AdmissionApplication) async throws -> AdmissionResponse {
        guard let url = URL(string: "\(baseURL)\(apiPrefix)/admissions") else {
            throw URLError(.badURL)
        }
        
        let body = try JSONEncoder().encode(application)
        let request = createRequest(url: url, method: "POST", body: body)
        
        let (data, _) = try await URLSession.shared.data(for: request)
        return try JSONDecoder().decode(AdmissionResponse.self, from: data)
    }
    
    // MARK: - Chat API
    private var cachedChatId: String?
    
    func ensureChatId() async throws -> String {
        if let cached = cachedChatId {
            return cached
        }
        
        // Получаем список чатов
        guard let chatsUrl = URL(string: "\(baseURL)\(apiPrefix)/chats") else {
            throw URLError(.badURL)
        }
        let chatsRequest = createRequest(url: chatsUrl)
        let (chatsData, _) = try await URLSession.shared.data(for: chatsRequest)
        let chats = try JSONDecoder().decode([Chat].self, from: chatsData)
        
        // Ищем открытый чат
        if let openChat = chats.first(where: { $0.status == "open" }) {
            cachedChatId = openChat.id
            return openChat.id
        }
        
        // Создаем новый чат
        guard let createUrl = URL(string: "\(baseURL)\(apiPrefix)/chats") else {
            throw URLError(.badURL)
        }
        let createRequest = createRequest(url: createUrl, method: "POST")
        let (createData, _) = try await URLSession.shared.data(for: createRequest)
        let createResponse = try JSONDecoder().decode(ChatCreateResponse.self, from: createData)
        cachedChatId = createResponse.id
        return createResponse.id
    }
    
    func fetchMessages() async throws -> [ChatMessage] {
        let chatId = try await ensureChatId()
        guard let url = URL(string: "\(baseURL)\(apiPrefix)/chats/\(chatId)/messages") else {
            throw URLError(.badURL)
        }
        
        let request = createRequest(url: url)
        let (data, _) = try await URLSession.shared.data(for: request)
        return try JSONDecoder().decode([ChatMessage].self, from: data)
    }
    
    func sendMessage(text: String) async throws {
        let chatId = try await ensureChatId()
        guard let url = URL(string: "\(baseURL)\(apiPrefix)/chats/\(chatId)/messages") else {
            throw URLError(.badURL)
        }
        
        let body = try JSONEncoder().encode(["text": text])
        let request = createRequest(url: url, method: "POST", body: body)
        
        let (_, _) = try await URLSession.shared.data(for: request)
        // Сервер возвращает { id: ... }, сообщения будут перезагружены отдельно
    }
    
    // MARK: - Settings API
    func fetchConsentText(language: String) async throws -> ConsentText {
        guard let url = URL(string: "\(baseURL)\(apiPrefix)/settings/consent/\(language)") else {
            throw URLError(.badURL)
        }
        
        let request = createRequest(url: url)
        let (data, _) = try await URLSession.shared.data(for: request)
        return try JSONDecoder().decode(ConsentText.self, from: data)
    }
    
    func fetchCountries() async throws -> [Country] {
        guard let url = URL(string: "\(baseURL)\(apiPrefix)/settings/countries") else {
            throw URLError(.badURL)
        }
        
        let request = createRequest(url: url)
        let (data, _) = try await URLSession.shared.data(for: request)
        let response = try JSONDecoder().decode(CountriesResponse.self, from: data)
        return response.countries.map { Country(id: $0, name: $0) }
    }
    
    // MARK: - i18n API
    func fetchTranslations(language: String) async throws -> [String: String] {
        guard let url = URL(string: "\(baseURL)\(apiPrefix)/i18n/\(language)") else {
            throw URLError(.badURL)
        }
        
        let request = createRequest(url: url)
        let (data, _) = try await URLSession.shared.data(for: request)
        return try JSONDecoder().decode([String: String].self, from: data)
    }
}

struct CountriesResponse: Codable {
    let countries: [String]
}

enum ApiError: Error {
    case badURL
    case httpError(Int)
    case serverError(String)
    case decodingError
    case networkError(Error)
}
