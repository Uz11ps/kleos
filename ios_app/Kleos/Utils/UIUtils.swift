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
                        // ВЕРХНИЙ КРУГ (Розовый #7E5074) - Эффект мягкого свечения
                        RadialGradient(
                            gradient: Gradient(colors: [Color(hex: "7E5074").opacity(0.9), Color(hex: "7E5074").opacity(0)]),
                            center: .center,
                            startRadius: 0,
                            endRadius: 160
                        )
                        .frame(width: 400, height: 400)
                        .position(x: geo.size.width / 2, y: 9)

                        // НИЖНИЙ КРУГ (Розовый #7E5074)
                        RadialGradient(
                            gradient: Gradient(colors: [Color(hex: "7E5074").opacity(0.9), Color(hex: "7E5074").opacity(0)]),
                            center: .center,
                            startRadius: 0,
                            endRadius: 160
                        )
                        .frame(width: 400, height: 400)
                        .position(x: geo.size.width / 2, y: geo.size.height - 9)
                    } else {
                        // Угловые свечения для внутренних страниц
                        RadialGradient(
                            gradient: Gradient(colors: [Color(hex: "7E5074").opacity(0.6), Color(hex: "7E5074").opacity(0)]),
                            center: .center,
                            startRadius: 0,
                            endRadius: 250
                        )
                        .frame(width: 600, height: 600)
                        .position(x: geo.size.width, y: 0)
                        
                        RadialGradient(
                            gradient: Gradient(colors: [Color(hex: "7E5074").opacity(0.6), Color(hex: "7E5074").opacity(0)]),
                            center: .center,
                            startRadius: 0,
                            endRadius: 250
                        )
                        .frame(width: 600, height: 600)
                        .position(x: 0, y: geo.size.height)
                    }
                    
                    // СИНИЙ ГРАДИЕНТ (как gradient_shape в Android)
                    if showGradientShape {
                        RadialGradient(
                            gradient: Gradient(colors: [Color(hex: "3B82F6").opacity(0.3), Color(hex: "3B82F6").opacity(0)]),
                            center: .center,
                            startRadius: 0,
                            endRadius: 200
                        )
                        .frame(width: 500, height: 500)
                        .position(x: 100, y: 100)
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
