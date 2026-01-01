import Foundation

// MARK: - News Models
struct NewsItem: Codable, Identifiable {
    let id: String
    let title: String
    let content: String?
    let imageUrl: String?
    let publishedAt: String
    let isInteresting: Bool?
    
    enum CodingKeys: String, CodingKey {
        case id = "_id"
        case title, content, imageUrl, publishedAt, isInteresting
    }
    
    var dateText: String {
        let formatter = ISO8601DateFormatter()
        if let date = formatter.date(from: publishedAt) {
            let displayFormatter = DateFormatter()
            displayFormatter.dateFormat = "dd.MM.yyyy"
            return displayFormatter.string(from: date)
        }
        return publishedAt
    }
}

// MARK: - User Models
struct UserProfile: Codable {
    let id: String
    let email: String
    let fullName: String
    let role: String
    let phone: String?
    let course: String?
    let speciality: String?
    let status: String?
    let university: String?
    let payment: String?
    let penalties: String?
    let notes: String?
    let studentId: String?
    let emailVerified: Bool
    let avatarUrl: String?
    
    enum CodingKeys: String, CodingKey {
        case id = "_id"
        case email, fullName, role, phone, course, speciality, status, university
        case payment, penalties, notes, studentId, emailVerified, avatarUrl
    }
}

struct UpdateProfileRequest: Codable {
    let fullName: String?
    let phone: String?
    let course: String?
    let speciality: String?
    let status: String?
    let university: String?
    let payment: String?
    let penalties: String?
    let notes: String?
}

// MARK: - University Models
struct University: Codable, Identifiable {
    let id: String
    let name: String
    let location: String
    let description: String?
    let website: String?
    let logoUrl: String?
    
    enum CodingKeys: String, CodingKey {
        case id = "_id"
        case name, location, description, website, logoUrl
    }
}

// MARK: - Program Models
struct Program: Codable, Identifiable {
    let id: String
    let name: String
    let description: String?
    let universityId: String?
    let universityName: String?
    let language: String?
    let educationLevel: String?
    let duration: String?
    
    enum CodingKeys: String, CodingKey {
        case id = "_id"
        case name, description, universityId, universityName, language, educationLevel, duration
    }
}

struct ProgramFilters: Codable {
    let language: String?
    let educationLevel: String?
    let universityId: String?
    let searchQuery: String?
}

// MARK: - Gallery Models
struct GalleryItem: Codable, Identifiable {
    let id: String
    let title: String
    let description: String?
    let mediaUrl: String
    let mediaType: String
    
    enum CodingKeys: String, CodingKey {
        case id = "_id"
        case title, description, mediaUrl, mediaType
    }
}

// MARK: - Partner Models
struct Partner: Codable, Identifiable {
    let id: String
    let name: String
    let description: String?
    let logoUrl: String?
    let website: String?
    
    enum CodingKeys: String, CodingKey {
        case id = "_id"
        case name, description, logoUrl, website
    }
}

// MARK: - Admission Models
struct AdmissionApplication: Codable {
    let firstName: String
    let lastName: String
    let patronymic: String?
    let email: String
    let phone: String
    let dateOfBirth: String
    let placeOfBirth: String
    let nationality: String
    let sex: String
    let passportNumber: String
    let passportIssue: String
    let passportExpiry: String
    let visaCity: String
    let program: String
    let comment: String?
}

struct AdmissionResponse: Codable {
    let id: String?
    let message: String?
    let error: String?
}

// MARK: - Chat Models
struct Chat: Codable, Identifiable {
    let id: String
    let status: String
    let lastMessageAt: String?
    
    enum CodingKeys: String, CodingKey {
        case id = "_id"
        case status, lastMessageAt
    }
}

struct ChatCreateResponse: Codable {
    let id: String
    
    enum CodingKeys: String, CodingKey {
        case id = "_id"
    }
}

struct ChatMessage: Codable, Identifiable {
    let id: String
    let text: String
    let senderRole: String // "student" or "admin"
    let createdAt: String
    
    enum CodingKeys: String, CodingKey {
        case id = "_id"
        case text, senderRole, createdAt
    }
    
    var sender: String {
        senderRole == "admin" ? "support" : "user"
    }
}

struct FAQItem: Codable, Identifiable {
    let id: String
    let question: String
    let answer: String
    
    var isExpanded: Bool = false
}

// MARK: - Auth Models
struct LoginRequest: Codable {
    let email: String
    let password: String
}

struct RegisterRequest: Codable {
    let fullName: String
    let email: String
    let password: String
}

struct AuthResponse: Codable {
    let token: String?
    let user: UserInfo?
    let requiresVerification: Bool?
    let verifyUrl: String?
    let appLink: String?
    let error: String?
}

struct UserInfo: Codable {
    let id: String
    let fullName: String
    let email: String
    let role: String
    
    enum CodingKeys: String, CodingKey {
        case id
        case fullName, email, role
    }
}

// MARK: - Settings Models
struct ConsentText: Codable {
    let text: String
}

struct Country: Codable, Identifiable {
    let id: String
    let name: String
    
    enum CodingKeys: String, CodingKey {
        case id = "_id"
        case name
    }
}
