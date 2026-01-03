import SwiftUI

// MARK: - Background View Modifier
struct KleosBackground: ViewModifier {
    var showGradientShape: Bool = false
    var circlePositions: CircleLayout = .corners
    var isSplashOrAuth: Bool = false
    
    enum CircleLayout {
        case center, corners
    }
    
    func body(content: Content) -> some View {
        ZStack {
            // 1. Основной фон - ТОЧНО #0E080F
            Color(hex: "0E080F")
                .ignoresSafeArea()
            
            // 2. Светящиеся слои (ТОЧНАЯ КОПИЯ ANDROID)
            GeometryReader { geo in
                ZStack {
                    if circlePositions == .center || isSplashOrAuth {
                        // ВЕРХНИЙ КРУГ (Розовый #7E5074)
                        // В Android: layout_marginTop="-150dp", width="318dp"
                        Circle()
                            .fill(Color(hex: "7E5074"))
                            .frame(width: 320, height: 320)
                            .blur(radius: 140) // Глубокий блюр
                            .opacity(0.8)
                            .position(x: geo.size.width / 2, y: -80)

                        // НИЖНИЙ КРУГ (Розовый #7E5074)
                        // В Android: layout_marginBottom="-150dp", width="318dp"
                        Circle()
                            .fill(Color(hex: "7E5074"))
                            .frame(width: 320, height: 320)
                            .blur(radius: 140)
                            .opacity(0.8)
                            .position(x: geo.size.width / 2, y: geo.size.height + 80)
                    } else {
                        // Угловые свечения для внутренних страниц
                        Circle()
                            .fill(Color(hex: "7E5074"))
                            .frame(width: 450, height: 450)
                            .blur(radius: 150)
                            .opacity(0.5)
                            .position(x: geo.size.width + 100, y: -100)
                        
                        Circle()
                            .fill(Color(hex: "7E5074"))
                            .frame(width: 450, height: 450)
                            .blur(radius: 150)
                            .opacity(0.5)
                            .position(x: -100, y: geo.size.height + 100)
                    }
                    
                    // СИНИЙ ГРАДИЕНТ (gradient_shape в Android)
                    if showGradientShape {
                        Circle()
                            .fill(Color(hex: "3B82F6").opacity(0.3))
                            .frame(width: 440, height: 440)
                            .blur(radius: 110)
                            .position(x: 0, y: 0)
                    }
                }
            }
            .ignoresSafeArea()
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
