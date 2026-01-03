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

// MARK: - Gradient Shape (Ribbon)
struct KleosRibbon: View {
    var body: some View {
        GeometryReader { geometry in
            ZStack {
                Path { path in
                    let w = geometry.size.width
                    let h = geometry.size.height
                    
                    // Улучшенная swirly-линия (лента)
                    path.move(to: CGPoint(x: w * -0.2, y: h * 0.4))
                    
                    path.addCurve(to: CGPoint(x: w * 0.4, y: h * 0.1),
                                 control1: CGPoint(x: w * 0.0, y: h * 0.5),
                                 control2: CGPoint(x: w * 0.2, y: h * -0.1))
                    
                    path.addCurve(to: CGPoint(x: w * 0.8, y: h * 0.6),
                                 control1: CGPoint(x: w * 0.6, y: h * 0.3),
                                 control2: CGPoint(x: w * w * 0.002, y: h * 0.8)) // w*w is just to make it wider
                    
                    path.addCurve(to: CGPoint(x: w * 1.2, y: h * 0.2),
                                 control1: CGPoint(x: w * 1.0, y: h * 0.4),
                                 control2: CGPoint(x: w * 1.1, y: h * 0.1))
                }
                .stroke(
                    LinearGradient(
                        gradient: Gradient(colors: [
                            Color(hex: "E8D5FF"), // Лавандовый
                            Color(hex: "D4A5FF"), // Светло-фиолетовый
                            Color(hex: "FFB6C1"), // Розовый
                            Color(hex: "FFD700")  // Желтый/золотой
                        ]),
                        startPoint: .leading,
                        endPoint: .trailing
                    ),
                    style: StrokeStyle(lineWidth: 120, lineCap: .round, lineJoin: .round)
                )
                .blur(radius: 60)
                .opacity(0.6)
            }
        }
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
                        // Auth/Splash: Центрированные пятна (по 60% от ширины экрана)
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
                            // Верхнее правое
                            BlurredCircle(color: Color(hex: "7E5074"), size: geo.size.width * 1.8)
                                .position(x: geo.size.width * 0.9, y: geo.size.height * 0.1)
                            
                            // Нижнее левое
                            BlurredCircle(color: Color(hex: "7E5074"), size: geo.size.width * 1.8)
                                .position(x: geo.size.width * 0.1, y: geo.size.height * 0.9)
                        }
                    }
                    
                    if showGradientShape {
                        // Градиентная "лента" (рибон) как в Android
                        KleosRibbon()
                            .frame(width: geo.size.width * 1.5, height: geo.size.height * 0.8)
                            .position(x: geo.size.width * 0.5, y: geo.size.height * 0.3)
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
        configuration.label.padding(.horizontal, 32).padding(.vertical, 16)
            .background(backgroundColor).foregroundColor(foregroundColor).cornerRadius(31)
            .scaleEffect(configuration.isPressed ? 0.95 : 1.0)
    }
}

struct KleosOutlinedButtonStyle: ButtonStyle {
    var strokeColor: Color = .white
    var foregroundColor: Color = .white
    func makeBody(configuration: Configuration) -> some View {
        configuration.label.padding(.horizontal, 32).padding(.vertical, 16)
            .overlay(RoundedRectangle(cornerRadius: 31).stroke(strokeColor, lineWidth: 2))
            .foregroundColor(foregroundColor).scaleEffect(configuration.isPressed ? 0.95 : 1.0)
    }
}
