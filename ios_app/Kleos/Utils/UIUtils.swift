import SwiftUI

// MARK: - Эффект зернистости (Noise)
struct NoiseModifier: ViewModifier {
    func body(content: Content) -> some View {
        content.overlay(
            ZStack {
                Color.white.opacity(0.01)
            }
            .allowsHitTesting(false)
        )
    }
}

// MARK: - Адаптивное свечение
struct BlurredCircle: View {
    var color: Color
    var size: CGFloat
    
    var body: some View {
        ZStack {
            RadialGradient(
                gradient: Gradient(colors: [
                    color.opacity(0.6),
                    color.opacity(0.25),
                    .clear
                ]),
                center: .center,
                startRadius: 0,
                endRadius: size * 0.5
            )
            
            Circle()
                .fill(color.opacity(0.15))
                .blur(radius: size * 0.2)
        }
        .frame(width: size, height: size)
        .blendMode(.screen)
    }
}

// MARK: - Gradient Shape (Ribbon Image)
struct KleosRibbon: View {
    var body: some View {
        Image("gradient_shape")
            .resizable()
            .scaledToFill()
            .opacity(1.0)
    }
}

// MARK: - Background View Modifier
struct KleosBackground: ViewModifier {
    var showGradientShape: Bool = false
    var circlePositions: CircleLayout = .corners
    var isSplashOrAuth: Bool = false
    
    enum CircleLayout {
        case center, corners
    }
    
    func body(content: Content) -> some View {
        GeometryReader { geo in
            ZStack {
                // 1. Фон
                (isSplashOrAuth ? Color(hex: "0E080F") : Color(hex: "0A0E1A"))
                    .ignoresSafeArea()
                
                // 2. Слои свечения (Адаптивные)
                Group {
                    if circlePositions == .center {
                        // Auth/Splash: Центрированные пятна
                        VStack {
                            BlurredCircle(color: Color(hex: "7E5074"), size: geo.size.width * 1.5)
                                .offset(y: -geo.size.height * 0.25)
                            Spacer()
                            BlurredCircle(color: Color(hex: "7E5074"), size: geo.size.width * 1.5)
                                .offset(y: geo.size.height * 0.25)
                        }
                    } else {
                        // Home/Main: Угловые пятна
                        ZStack {
                            BlurredCircle(color: Color(hex: "7E5074"), size: geo.size.width * 1.8)
                                .position(x: geo.size.width * 0.9, y: geo.size.height * 0.1)
                            
                            BlurredCircle(color: Color(hex: "7E5074"), size: geo.size.width * 1.8)
                                .position(x: geo.size.width * 0.1, y: geo.size.height * 0.9)
                        }
                    }
                    
                    if showGradientShape {
                        // Картинка ленты в верхней части экрана
                        KleosRibbon()
                            .frame(width: geo.size.width * 1.2) // Чуть шире экрана
                            .position(x: geo.size.width * 0.6, y: geo.size.height * 0.15)
                            .allowsHitTesting(false)
                    }
                }
                .ignoresSafeArea()
                .modifier(NoiseModifier())
                .allowsHitTesting(false)
                
                content
            }
            .frame(width: geo.size.width, height: geo.size.height)
        }
        .ignoresSafeArea()
    }
}

extension View {
    func kleosBackground(showGradientShape: Bool = false, circlePositions: KleosBackground.CircleLayout = .corners, isSplashOrAuth: Bool = false) -> some View {
        modifier(KleosBackground(showGradientShape: showGradientShape, circlePositions: circlePositions, isSplashOrAuth: isSplashOrAuth))
    }
}

// Вспомогательные компоненты
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
        self.init(.sRGB, red: Double(r) / 255, green: Double(g) / 255, blue:  Double(b) / 255, opacity: Double(a) / 255)
    }
    
    static let kleosBackground = Color(hex: "0A0E1A")
    static let kleosPurple = Color(hex: "8B5CF6")
    static let kleosBlue = Color(hex: "3B82F6")
    static let kleosYellow = Color(hex: "FFD600")
    static let kleosPinkAccent = Color(hex: "FF6B9D")
}

struct LoadingView: View {
    var body: some View {
        ProgressView().tint(.white).frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

struct CategoryBadge: View {
    let text: String
    let isInteresting: Bool
    var body: some View {
        Text(text).font(.system(size: 12, weight: .bold)).padding(.horizontal, 8).padding(.vertical, 4)
            .background(isInteresting ? Color.kleosYellow : Color(hex: "D4B5FF")).cornerRadius(4)
            .foregroundColor(isInteresting ? .black : .white)
    }
}

struct KleosButtonStyle: ButtonStyle {
    var backgroundColor: Color = .white
    var foregroundColor: Color = .black
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .background(backgroundColor)
            .foregroundColor(foregroundColor)
            .clipShape(Capsule())
            .scaleEffect(configuration.isPressed ? 0.98 : 1.0)
    }
}

struct KleosOutlinedButtonStyle: ButtonStyle {
    var strokeColor: Color = .white
    var foregroundColor: Color = .white
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .foregroundColor(foregroundColor)
            .background(
                Capsule()
                    .stroke(strokeColor, lineWidth: 2)
            )
            .scaleEffect(configuration.isPressed ? 0.98 : 1.0)
    }
}
