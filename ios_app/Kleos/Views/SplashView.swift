import SwiftUI

struct SplashView: View {
    @ObservedObject private var sessionManager = SessionManager.shared
    @State private var isSplashFinished = false
    @AppStorage("has_seen_onboarding") var hasSeenOnboarding: Bool = false
    
    var body: some View {
        ZStack {
            // Фон всегда один
            Color(hex: "0E080F").ignoresSafeArea()
            
            if !isSplashFinished {
                // КЛЕОС ЦЕНТРАЛИЗОВАННЫЙ
                VStack {
                    Spacer()
                    Text("Kleos")
                        .font(.system(size: 64, weight: .bold))
                        .foregroundColor(.white)
                        .tracking(2)
                    Spacer()
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .kleosBackground(showGradientShape: false, circlePositions: .center, isSplashOrAuth: true)
                .transition(.opacity)
            } else {
                // Основная логика навигации
                if !hasSeenOnboarding {
                    OnboardingView {
                        withAnimation {
                            hasSeenOnboarding = true
                        }
                    }
                    .transition(.asymmetric(insertion: .move(edge: .trailing), removal: .opacity))
                } else if sessionManager.isLoggedIn {
                    MainTabView()
                        .transition(.opacity)
                        .id("MainApp") // Force re-render when switching
                } else {
                    AuthView()
                        .transition(.opacity)
                        .id("AuthFlow")
                }
            }
        }
        .onAppear {
            DispatchQueue.main.asyncAfter(deadline: .now() + 2) {
                withAnimation(.easeInOut(duration: 0.8)) {
                    isSplashFinished = true
                }
            }
        }
    }
}

struct OnboardingView: View {
    let onComplete: () -> Void
    
    var body: some View {
        ZStack {
            VStack {
                Spacer()
                
                // Текст в левом нижнем углу
                VStack(alignment: .leading, spacing: 12) {
                    Text(LocalizationManager.shared.t("study"))
                        .font(.system(size: 32, weight: .regular))
                        .foregroundColor(.white)
                    
                    Text(LocalizationManager.shared.t("programs_and"))
                        .font(.system(size: 32, weight: .regular))
                        .foregroundColor(.white)
                    
                    Text(LocalizationManager.shared.t("submit_apps"))
                        .font(.system(size: 32, weight: .regular))
                        .foregroundColor(.white)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.leading, 24)
                .padding(.bottom, 180)
                
                // Кнопка со стрелкой внизу по центру
                Button(action: {
                    onComplete()
                }) {
                    ZStack {
                        Circle()
                            .fill(Color.white)
                            .frame(width: 108, height: 108)
                        
                        Image(systemName: "arrow.right")
                            .font(.system(size: 32, weight: .bold))
                            .foregroundColor(.black)
                    }
                }
                .padding(.bottom, 44)
            }
        }
        .kleosBackground(showGradientShape: true, circlePositions: .corners, isSplashOrAuth: true)
    }
}
