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
struct SocialLinks: Codable {
    let facebook: String?
    let twitter: String?
    let instagram: String?
    let youtube: String?
    let whatsapp: String?
    let phone: String?
    let email: String?
}

struct DegreeProgram: Codable {
    let type: String
    let description: String?
}

struct ContentBlock: Codable {
    let type: String
    let content: String?
    let order: Int?
}

struct University: Codable, Identifiable {
    let id: String
    let name: String
    let city: String?
    let country: String?
    let description: String?
    let website: String?
    let logoUrl: String?
    let socialLinks: SocialLinks?
    let degreePrograms: [DegreeProgram]?
    let contentBlocks: [ContentBlock]?
    
    enum CodingKeys: String, CodingKey {
        case id = "_id"
        case name, city, country, description, website, logoUrl, socialLinks, degreePrograms, contentBlocks
    }
    
    var location: String {
        var parts: [String] = []
        if let city = city { parts.append(city) }
        if let country = country { parts.append(country) }
        return parts.joined(separator: ", ")
    }
}

// MARK: - Program Models
struct Program: Codable, Identifiable {
    let id: String
    let title: String
    let description: String?
    let language: String?
    let level: String? // "Bachelor's degree", "Master's degree", "Research degree", "Speciality degree"
    let university: String? // Legacy field for backward compatibility
    let universityId: String? // New field linking to University model
    let tuition: Double?
    let durationYears: Double? // Changed from durationMonths to durationYears
    let active: Bool?
    let order: Int?
    
    enum CodingKeys: String, CodingKey {
        case id = "_id"
        case title, description, language, level, university, universityId, tuition, durationYears, active, order
    }
    
    var name: String { title }
    var educationLevel: String? { level }
    var duration: String? {
        guard let years = durationYears else { return nil }
        return String(format: "%.1f years", years)
    }
}

struct ProgramFilters: Codable {
    let language: String?
    let level: String?
    let university: String?
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
    let phone: String
    let email: String
    let dateOfBirth: String?
    let placeOfBirth: String?
    let nationality: String?
    let passportNumber: String?
    let passportIssue: String?
    let passportExpiry: String?
    let visaCity: String?
    let program: String
    let comment: String?
}

struct AdmissionResponse: Codable {
    let ok: Bool?
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
