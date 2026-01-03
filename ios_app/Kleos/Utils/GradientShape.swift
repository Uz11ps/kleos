import SwiftUI

struct GradientShape: View {
    var body: some View {
        GeometryReader { geometry in
            ZStack {
                // Градиентная форма как в Android
                Path { path in
                    let width = geometry.size.width
                    let height = geometry.size.height
                    
                    // Начинаем с верхней части формы (левая сторона)
                    path.move(to: CGPoint(x: width * 0.25, y: height * 0.08))
                    
                    // Верхняя левая петля
                    path.addCurve(
                        to: CGPoint(x: width * 0.1, y: height * 0.25),
                        control1: CGPoint(x: width * 0.15, y: height * 0.02),
                        control2: CGPoint(x: width * 0.08, y: height * 0.12)
                    )
                    
                    path.addCurve(
                        to: CGPoint(x: width * 0.25, y: height * 0.4),
                        control1: CGPoint(x: width * 0.12, y: height * 0.35),
                        control2: CGPoint(x: width * 0.18, y: height * 0.38)
                    )
                    
                    // Средняя часть (соединяющая)
                    path.addCurve(
                        to: CGPoint(x: width * 0.75, y: height * 0.4),
                        control1: CGPoint(x: width * 0.4, y: height * 0.42),
                        control2: CGPoint(x: width * 0.6, y: height * 0.42)
                    )
                    
                    // Нижняя правая петля
                    path.addCurve(
                        to: CGPoint(x: width * 0.9, y: height * 0.25),
                        control1: CGPoint(x: width * 0.82, y: height * 0.38),
                        control2: CGPoint(x: width * 0.88, y: height * 0.35)
                    )
                    
                    path.addCurve(
                        to: CGPoint(x: width * 0.75, y: height * 0.08),
                        control1: CGPoint(x: width * 0.92, y: height * 0.12),
                        control2: CGPoint(x: width * 0.85, y: height * 0.02)
                    )
                    
                    // Верхняя правая часть
                    path.addCurve(
                        to: CGPoint(x: width * 0.5, y: height * 0.15),
                        control1: CGPoint(x: width * 0.65, y: height * 0.1),
                        control2: CGPoint(x: width * 0.55, y: height * 0.12)
                    )
                    
                    // Замыкаем путь
                    path.addCurve(
                        to: CGPoint(x: width * 0.25, y: height * 0.08),
                        control1: CGPoint(x: width * 0.45, y: height * 0.12),
                        control2: CGPoint(x: width * 0.35, y: height * 0.1)
                    )
                }
                .fill(
                    LinearGradient(
                        gradient: Gradient(colors: [
                            Color(hex: "E8D5FF"), // Лавандовый
                            Color(hex: "D4A5FF"), // Светло-фиолетовый
                            Color(hex: "FFB6C1"), // Розовый
                            Color(hex: "FFD700")  // Желтый/золотой
                        ]),
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                )
            }
        }
    }
}

