import SwiftUI

// MARK: - Blurred Circle View
struct BlurredCircle: View {
    var color: Color = Color(hex: "7E5074").opacity(0.4)
    var size: CGFloat = 318
    var blurRadius: CGFloat = 50
    
    var body: some View {
        Circle()
            .fill(color)
            .frame(width: size, height: size)
            .blur(radius: blurRadius)
    }
}

// MARK: - Color Extensions
extension Color {
    init(hex: String) {
        let hex = hex.trimmingCharacters(in: CharacterSet.alphanumerics.inverted)
        var int: UInt64 = 0
        Scanner(string: hex).scanHexInt64(&int)
        let a, r, g, b: UInt64
        switch hex.count {
        case 3: // RGB (12-bit)
            (a, r, g, b) = (255, (int >> 8) * 17, (int >> 4 & 0xF) * 17, (int & 0xF) * 17)
        case 6: // RGB (24-bit)
            (a, r, g, b) = (255, int >> 16, int >> 8 & 0xFF, int & 0xFF)
        case 8: // ARGB (32-bit)
            (a, r, g, b) = (int >> 24, int >> 16 & 0xFF, int >> 8 & 0xFF, int & 0xFF)
        default:
            (a, r, g, b) = (1, 1, 1, 0)
        }

        self.init(
            .sRGB,
            red: Double(r) / 255,
            green: Double(g) / 255,
            blue:  Double(b) / 255,
            opacity: Double(a) / 255
        )
    }
    
    static let kleosBackground = Color(hex: "0F172A")
    static let kleosAccent = Color(hex: "7E5074")
    static let kleosBlue = Color(hex: "3B82F6")
}

// MARK: - Background View Modifier
struct KleosBackground: ViewModifier {
    func body(content: Content) -> some View {
        ZStack {
            Color.kleosBackground.ignoresSafeArea()
            
            // Background blurred circles
            VStack {
                HStack {
                    BlurredCircle()
                        .offset(x: -100, y: -100)
                    Spacer()
                }
                Spacer()
                HStack {
                    Spacer()
                    BlurredCircle(color: Color.kleosBlue.opacity(0.3))
                        .offset(x: 100, y: 100)
                }
            }
            .ignoresSafeArea()
            
            content
        }
    }
}

extension View {
    func kleosBackground() -> some View {
        modifier(KleosBackground())
    }
}

// MARK: - Custom Button Styles
struct KleosButtonStyle: ButtonStyle {
    var backgroundColor: Color = .white
    var foregroundColor: Color = .black
    
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .padding(.horizontal, 32)
            .padding(.vertical, 16)
            .background(backgroundColor)
            .foregroundColor(foregroundColor)
            .cornerRadius(31)
            .scaleEffect(configuration.isPressed ? 0.95 : 1.0)
            .animation(.easeInOut(duration: 0.1), value: configuration.isPressed)
    }
}

struct KleosOutlinedButtonStyle: ButtonStyle {
    var strokeColor: Color = .white
    var foregroundColor: Color = .white
    
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .padding(.horizontal, 32)
            .padding(.vertical, 16)
            .background(Color.clear)
            .foregroundColor(foregroundColor)
            .overlay(
                RoundedRectangle(cornerRadius: 31)
                    .stroke(strokeColor, lineWidth: 2)
            )
            .scaleEffect(configuration.isPressed ? 0.95 : 1.0)
            .animation(.easeInOut(duration: 0.1), value: configuration.isPressed)
    }
}

// MARK: - Category Badge
struct CategoryBadge: View {
    let text: String
    let isInteresting: Bool
    
    var body: some View {
        Text(text)
            .font(.system(size: 12, weight: .bold))
            .padding(.horizontal, 8)
            .padding(.vertical, 4)
            .background(isInteresting ? Color.purple.opacity(0.6) : Color.blue.opacity(0.6))
            .foregroundColor(.white)
            .cornerRadius(4)
    }
}

// MARK: - Loading View
struct LoadingView: View {
    var body: some View {
        ProgressView()
            .tint(.white)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

// MARK: - Error View
struct ErrorView: View {
    let message: String
    let retry: (() -> Void)?
    
    var body: some View {
        VStack(spacing: 16) {
            Text(message)
                .foregroundColor(.white)
                .multilineTextAlignment(.center)
            
            if let retry = retry {
                Button("Retry", action: retry)
                    .buttonStyle(KleosButtonStyle())
            }
        }
        .padding()
    }
}
