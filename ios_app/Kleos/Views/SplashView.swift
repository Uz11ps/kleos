import SwiftUI

struct SplashView: View {
    @StateObject private var sessionManager = SessionManager.shared
    @State private var isActive = false
    @State private var showOnboarding = false
    
    var body: some View {
        VStack {
            Spacer()
            Text("Kleos")
                .font(.system(size: 48, weight: .bold))
                .foregroundColor(.white)
            Spacer()
        }
        .kleosBackground() // Централизованный фон
        .onAppear {

            // Проверяем, показывали ли onboarding
            let hasSeenOnboarding = UserDefaults.standard.bool(forKey: "has_seen_onboarding")
            
            DispatchQueue.main.asyncAfter(deadline: .now() + 2) {
                if !hasSeenOnboarding {
                    showOnboarding = true
                } else {
                    isActive = true
                }
            }
        }
        .fullScreenCover(isPresented: $showOnboarding) {
            OnboardingView {
                UserDefaults.standard.set(true, forKey: "has_seen_onboarding")
                isActive = true
            }
        }
        .fullScreenCover(isPresented: $isActive) {
            if sessionManager.isLoggedIn {
                MainTabView()
            } else {
                AuthView()
            }
        }
    }
}

struct OnboardingView: View {
    let onComplete: () -> Void
    
    @State private var currentPage = 0
    
    let pages = [
        OnboardingPage(
            title: "Discover and explore famed Russian universities",
            subtitle: "Through our mobile application, you will be able to discover first‑rate & notable Russian universities and all their programs"
        ),
        OnboardingPage(
            title: "StudyInRussia Awesome\nTalk Show and Broadcast",
            subtitle: "The distinctive feature of our mobile application which enables you to watch live talk show & broadcast with leading Russian universities to discuss and obtain the latest"
        ),
        OnboardingPage(
            title: "Forum, the excellent venue for study discussion and social interaction",
            subtitle: "Forum, the outstanding and designated space created with the intention for you to join, share and post the brilliant ideas in your mind to interact with each other"
        )
    ]
    
    var body: some View {
        TabView(selection: $currentPage) {
            ForEach(0..<pages.count, id: \.self) { index in
                OnboardingPageView(page: pages[index])
                    .tag(index)
            }
        }
        .tabViewStyle(.page)
        .indexViewStyle(.page(backgroundDisplayMode: .always))
        .overlay(
            VStack {
                Spacer()
                HStack {
                    if currentPage > 0 {
                        Button("Back") {
                            withAnimation {
                                currentPage -= 1
                            }
                        }
                        .buttonStyle(KleosOutlinedButtonStyle())
                    }
                    
                    Spacer()
                    
                    if currentPage < pages.count - 1 {
                        Button("Next") {
                            withAnimation {
                                currentPage += 1
                            }
                        }
                        .buttonStyle(KleosButtonStyle())
                    } else {
                        Button("Start") {
                            onComplete()
                        }
                        .buttonStyle(KleosButtonStyle())
                    }
                }
                .padding()
            }
        )
        .kleosBackground() // Централизованный фон
    }
}


struct OnboardingPage {
    let title: String
    let subtitle: String
}

struct OnboardingPageView: View {
    let page: OnboardingPage
    
    var body: some View {
        VStack(spacing: 24) {
            Spacer()
            
            Text(page.title)
                .font(.system(size: 32, weight: .bold))
                .foregroundColor(.white)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 24)
            
            Text(page.subtitle)
                .font(.system(size: 16))
                .foregroundColor(.gray)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 24)
            
            Spacer()
        }
    }
}

