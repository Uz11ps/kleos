import SwiftUI

extension Color {
    init(hex: String) {
        let hex = hex.trimmingCharacters(in: CharacterSet.alphanumerics.inverted)
        var int: UInt64 = 0
        Scanner(string: hex).scanHexInt64(&int)
        let a, r, g, b: UInt64
        switch hex.count {
        case 3: (a, r, g, b) = (255, (int >> 8) * 17, (int >> 4 & 0xF) * 17, (int & 0xF) * 17)
        case 6: (a, r, g, b) = (255, int >> 16, int >> 8 & 0xFF, int & 0xFF)
        case 8: (a, r, g, b) = (int >> 24, int >> 16 & 0xFF, int >> 8 & 0xFF, int & 0xFF)
        default: (a, r, g, b) = (255, 0, 0, 0)
        }
        self.init(.sRGB, red: Double(r) / 255, green: Double(g) / 255, blue: Double(b) / 255, opacity: Double(a) / 255)
    }
    
    static let kleosBackground = Color(hex: "0E080F")
    static let kleosBlue = Color(hex: "3B82F6")
}

struct BlurredCircle: View {
    let color: Color
    var body: some View {
        RadialGradient(
            gradient: Gradient(colors: [color.opacity(0.8), color.opacity(0.3), .clear]),
            center: .center,
            startRadius: 0,
            endRadius: 150
        )
        .frame(width: 320, height: 320)
        .blur(radius: 80) // Глубокое размытие как в Android
    }
}

struct KleosBackground: ViewModifier {
    var showGradientShape: Bool = false
    var circlePositions: CirclePositions = .standard
    var isSplashOrAuth: Bool = false
    
    enum CirclePositions { case standard, center }

    func body(content: Content) -> some View {
        ZStack {
            // Базовый цвет фона (Android onboarding_background)
            Color.kleosBackground
                .ignoresSafeArea()
            
            GeometryReader { geometry in
                ZStack {
                    // Синее свечение в левом верхнем углу (как gradient_shape в Android)
                    RadialGradient(
                        gradient: Gradient(colors: [Color.kleosBlue.opacity(0.4), .clear]),
                        center: .topLeading,
                        startRadius: 0,
                        endRadius: 250
                    )
                    .offset(x: -50, y: -50)
                    .ignoresSafeArea()
                    
                    // Верхнее розовое пятно
                    BlurredCircle(color: Color(hex: "D946EF").opacity(0.5))
                        .position(x: geometry.size.width * 0.7, y: 10) // 9 пунктов от края
                    
                    // Нижнее розовое пятно
                    BlurredCircle(color: Color(hex: "D946EF").opacity(0.4))
                        .position(x: geometry.size.width * 0.3, y: geometry.size.height - 10)
                }
            }
            .ignoresSafeArea()
            
            content
        }
    }
}

extension View {
    func kleosBackground(showGradientShape: Bool = true, circlePositions: KleosBackground.CirclePositions = .standard, isSplashOrAuth: Bool = false) -> some View {
        self.modifier(KleosBackground(showGradientShape: showGradientShape, circlePositions: circlePositions, isSplashOrAuth: isSplashOrAuth))
    }
}

// Стандартные стили карточек
extension View {
    func kleosCardStyle() -> some View {
        self.background(Color.white.opacity(0.05))
            .cornerRadius(20)
            .overlay(
                RoundedRectangle(cornerRadius: 20)
                    .stroke(Color.white.opacity(0.1), lineWidth: 1)
            )
    }
}

struct KleosButtonStyle: ButtonStyle {
    var backgroundColor: Color
    var foregroundColor: Color
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .padding()
            .frame(maxWidth: .infinity)
            .background(backgroundColor)
            .foregroundColor(foregroundColor)
            .cornerRadius(16)
            .scaleEffect(configuration.isPressed ? 0.98 : 1.0)
            .opacity(configuration.isPressed ? 0.9 : 1.0)
    }
}

struct KleosOutlinedButtonStyle: ButtonStyle {
    var strokeColor: Color
    var foregroundColor: Color
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .padding()
            .frame(maxWidth: .infinity)
            .background(Color.clear)
            .foregroundColor(foregroundColor)
            .overlay(
                RoundedRectangle(cornerRadius: 16)
                    .stroke(strokeColor, lineWidth: 2)
            )
            .scaleEffect(configuration.isPressed ? 0.98 : 1.0)
    }
}
