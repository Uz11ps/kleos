import SwiftUI
import Combine

// MARK: - Phone Mask Formatter
class PhoneMaskFormatter: ObservableObject {
    @Published var text: String = ""
    
    private let countryPrefix: String
    
    init(languageCode: String = "en") {
        switch languageCode.lowercased() {
        case "ru":
            self.countryPrefix = "+7"
        case "en":
            self.countryPrefix = "+1"
        case "zh":
            self.countryPrefix = "+86"
        default:
            self.countryPrefix = "+7"
        }
    }
    
    func format(_ input: String) -> String {
        let digits = input.filter { $0.isNumber }
        let ccDigits = countryPrefix.filter { $0.isNumber }
        
        var normalized = digits
        if !normalized.hasPrefix(ccDigits) {
            normalized = String(normalized.trimmingPrefix(while: { $0 == "0" }))
            normalized = ccDigits + normalized
        }
        
        var result = countryPrefix + "-"
        var index = ccDigits.count
        
        func take(_ count: Int) -> String {
            let end = min(index + count, normalized.count)
            guard index < end else { return "" }
            let part = String(normalized[normalized.index(normalized.startIndex, offsetBy: index)..<normalized.index(normalized.startIndex, offsetBy: end)])
            index = end
            return part
        }
        
        let p1 = take(3)
        if !p1.isEmpty {
            result += "(" + p1
            if p1.count == 3 {
                result += ")"
            } else {
                return result
            }
        } else {
            return result
        }
        
        let p2 = take(3)
        if !p2.isEmpty {
            result += "-" + p2
            if p2.count < 3 {
                return result
            }
        } else {
            return result
        }
        
        let p3 = take(2)
        if !p3.isEmpty {
            result += "-" + p3
            if p3.count < 2 {
                return result
            }
        } else {
            return result
        }
        
        let p4 = take(2)
        if !p4.isEmpty {
            result += "-" + p4
        }
        
        return result
    }
}

// MARK: - Date Mask Formatter
class DateMaskFormatter: ObservableObject {
    @Published var text: String = ""
    
    func format(_ input: String) -> String {
        let digits = input.filter { $0.isNumber }
        var result = ""
        
        for (index, digit) in digits.enumerated() {
            if index >= 8 { break }
            result.append(digit)
            if (index == 1 || index == 3) && index != digits.count - 1 {
                result.append(".")
            }
        }
        
        return result
    }
}

// MARK: - TextField with Mask Modifier
struct MaskedTextField: View {
    @Binding var text: String
    let placeholder: String
    let maskType: MaskType
    let keyboardType: UIKeyboardType
    
    enum MaskType {
        case phone
        case date
        case none
    }
    
    private var languageCode: String {
        Locale.current.language.languageCode?.identifier ?? "en"
    }
    
    @StateObject private var phoneFormatter: PhoneMaskFormatter
    @StateObject private var dateFormatter = DateMaskFormatter()
    
    init(text: Binding<String>, placeholder: String, maskType: MaskType, keyboardType: UIKeyboardType) {
        self._text = text
        self.placeholder = placeholder
        self.maskType = maskType
        self.keyboardType = keyboardType
        
        let langCode = Locale.current.language.languageCode?.identifier ?? "en"
        _phoneFormatter = StateObject(wrappedValue: PhoneMaskFormatter(languageCode: langCode))
    }
    
    var body: some View {
        TextField(placeholder, text: $text)
            .textFieldStyle(KleosTextFieldStyle())
            .keyboardType(keyboardType)
            .onChange(of: text) { oldValue, newValue in
                switch maskType {
                case .phone:
                    let formatted = phoneFormatter.format(newValue)
                    if formatted != newValue {
                        text = formatted
                    }
                case .date:
                    let formatted = dateFormatter.format(newValue)
                    if formatted != newValue {
                        text = formatted
                    }
                case .none:
                    break
                }
            }
    }
}

extension StringProtocol {
    func trimmingPrefix(while predicate: (Character) -> Bool) -> SubSequence {
        var start = startIndex
        while start < endIndex && predicate(self[start]) {
            start = index(after: start)
        }
        return self[start...]
    }
}

