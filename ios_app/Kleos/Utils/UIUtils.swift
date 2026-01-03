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
        Circle()
            .fill(color)
            .frame(width: size, height: size)
            .blur(radius: size * 0.3)
            .opacity(0.5)
    }
}

// MARK: - Background View Modifier
struct KleosBackground: ViewModifier {
    var showGradientShape: Bool = false
    var circlePositions: CircleLayout = .corners
    var isSplashOrAuth: Bool = false // Добавил обратно для совместимости
    
    enum CircleLayout {
        case center, corners
    }
    
    func body(content: Content) -> some View {
        ZStack {
            // 1. Основной фон - точно как в Android (onboarding_background)
            Color(hex: "0E080F")
                .ignoresSafeArea()
            
            // 2. Слои свечения (точно как в Android layout)
            Group {
                if circlePositions == .center || isSplashOrAuth {
                    // Расположение для Auth/Splash (по центру сверху и снизу)
                    VStack {
                        BlurredCircle(color: Color(hex: "7E5074"), size: 318)
                            .offset(y: -150)
                        Spacer()
                        BlurredCircle(color: Color(hex: "7E5074"), size: 318)
                            .offset(y: 150)
                    }
                } else {
                    // Расположение для остальных экранов (углы)
                    VStack {
                        HStack {
                            Spacer()
                            BlurredCircle(color: Color(hex: "7E5074"), size: 400)
                                .offset(x: 100, y: -100)
                        }
                        Spacer()
                        HStack {
                            BlurredCircle(color: Color(hex: "7E5074"), size: 400)
                                .offset(x: -100, y: 100)
                            Spacer()
                        }
                    }
                }
                
                // Синее свечение (gradientShape из Android)
                if showGradientShape {
                    VStack {
                        HStack {
                            BlurredCircle(color: Color(hex: "3B82F6"), size: 400)
                                .opacity(0.2)
                                .offset(x: -150, y: -150)
                            Spacer()
                        }
                        Spacer()
                    }
                }
            }
            .ignoresSafeArea()
            .modifier(NoiseModifier())
            .allowsHitTesting(false)
            
            content
        }
    }
}

extension View {
    func kleosBackground(showGradientShape: Bool = false, circlePositions: KleosBackground.CircleLayout = .corners, isSplashOrAuth: Bool = false) -> some View {
        modifier(KleosBackground(showGradientShape: showGradientShape, circlePositions: circlePositions, isSplashOrAuth: isSplashOrAuth))
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
        case 3: (a, r, g, b) = (255, (int >> 8) * 17, (int >> 4 & 0xF) * 17, (int & 0xF) * 17)
        case 6: (a, r, g, b) = (255, int >> 16, int >> 8 & 0xFF, int & 0xFF)
        case 8: (a, r, g, b) = (int >> 24, int >> 16 & 0xFF, int >> 8 & 0xFF, int & 0xFF)
        default: (a, r, g, b) = (255, 0, 0, 0)
        }
        self.init(.sRGB, red: Double(r) / 255, green: Double(g) / 255, blue:  Double(b) / 255, opacity: Double(a) / 255)
    }
    
    static let kleosBackground = Color(hex: "0E080F")
    static let kleosPurple = Color(hex: "8B5CF6")
    static let kleosBlue = Color(hex: "3B82F6")
    static let kleosYellow = Color(hex: "FFD600")
}

// MARK: - UI Components
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
