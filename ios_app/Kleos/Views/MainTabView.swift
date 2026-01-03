import SwiftUI

struct MainTabView: View {
    @StateObject private var sessionManager = SessionManager.shared
    @StateObject private var localizationManager = LocalizationManager.shared
    @State private var selectedTab = 0
    @State private var showDrawer = false
    @State private var activeSheet: ActiveSheet?
    
    enum ActiveSheet: Identifiable {
        case profile, admission, support, news, programs, partners, universities
        var id: Int { hashValue }
    }
    
    var isGuest: Bool {
        sessionManager.isGuest()
    }
    
    var body: some View {
        ZStack {
            TabView(selection: $selectedTab) {
                if isGuest {
                    NavigationView {
                        HomeView()
                            .navigationBarTitleDisplayMode(.inline)
                            .toolbar {
                                ToolbarItem(placement: .navigationBarLeading) {
                                    Button(action: { withAnimation { showDrawer = true } }) {
                                        Image(systemName: "line.3.horizontal").foregroundColor(.white)
                                    }
                                }
                            }
                    }
                    .tabItem { Label(t("home"), systemImage: "house.fill") }.tag(0)
                    
                    NavigationView {
                        GalleryView()
                    }
                    .tabItem { Label(t("gallery"), systemImage: "photo.on.rectangle.angled") }.tag(1)
                } else {
                    NavigationView {
                        UniversitiesView()
                            .navigationBarTitleDisplayMode(.inline)
                            .toolbar {
                                ToolbarItem(placement: .navigationBarLeading) {
                                    Button(action: { withAnimation { showDrawer = true } }) {
                                        Image(systemName: "line.3.horizontal").foregroundColor(.white)
                                    }
                                }
                            }
                    }
                    .tabItem { Label(t("universities"), systemImage: "graduationcap.fill") }.tag(0)
                    
                    NavigationView {
                        HomeView()
                            .navigationBarTitleDisplayMode(.inline)
                            .toolbar {
                                ToolbarItem(placement: .navigationBarLeading) {
                                    Button(action: { withAnimation { showDrawer = true } }) {
                                        Image(systemName: "line.3.horizontal").foregroundColor(.white)
                                    }
                                }
                            }
                    }
                    .tabItem { Label(t("home"), systemImage: "house.fill") }.tag(1)
                    
                    NavigationView {
                        GalleryView()
                    }
                    .tabItem { Label(t("gallery"), systemImage: "photo.on.rectangle.angled") }.tag(2)
                }
            }
            .accentColor(Color.kleosPurple)
            .onAppear {
                setupTabBarAppearance()
                // Проверяем действие при появлении, если оно уже было установлено
                if sessionManager.deepLinkAction == .openProfile {
                    print("📱 MainTabView onAppear: deepLinkAction is .openProfile")
                    DispatchQueue.main.asyncAfter(deadline: .now() + 1.2) {
                        withAnimation {
                            self.activeSheet = .profile
                            sessionManager.deepLinkAction = nil
                        }
                    }
                }
            }
            
            if showDrawer {
                DrawerMenuView(isPresented: $showDrawer, onNavigate: { sheet in
                    self.activeSheet = sheet
                })
                .transition(.move(edge: .leading))
                .zIndex(10)
            }
        }
        .sheet(item: $activeSheet) { item in
            NavigationView {
                switch item {
                case .profile: ProfileView()
                case .admission: AdmissionView()
                case .support: ChatView()
                case .news: NewsView()
                case .programs: ProgramsView()
                case .partners: PartnersView()
                case .universities: UniversitiesView()
                }
            }
        }
        .onReceive(sessionManager.$deepLinkAction) { action in
            if action == .openProfile {
                print("📱 MainTabView onReceive: deepLinkAction is .openProfile")
                // Небольшая задержка, чтобы SwiftUI успел отрисовать основной экран и закрыть Splash/Auth
                DispatchQueue.main.asyncAfter(deadline: .now() + 1.2) {
                    withAnimation {
                        self.activeSheet = .profile
                        sessionManager.deepLinkAction = nil
                    }
                }
            }
        }
    }
    
    private func setupTabBarAppearance() {
        let appearance = UITabBarAppearance()
        appearance.configureWithTransparentBackground()
        appearance.backgroundColor = .clear
        UITabBar.appearance().standardAppearance = appearance
        UITabBar.appearance().scrollEdgeAppearance = appearance
        
        let navAppearance = UINavigationBarAppearance()
        navAppearance.configureWithTransparentBackground()
        navAppearance.backgroundColor = .clear
        UINavigationBar.appearance().standardAppearance = navAppearance
        UINavigationBar.appearance().scrollEdgeAppearance = navAppearance
    }
}

struct DrawerMenuView: View {
    @Binding var isPresented: Bool
    @StateObject private var localizationManager = LocalizationManager.shared
    let onNavigate: (MainTabView.ActiveSheet) -> Void
    @StateObject private var sessionManager = SessionManager.shared
    
    var body: some View {
        ZStack {
            Color.black.opacity(0.5)
                .ignoresSafeArea()
                .onTapGesture { withAnimation { isPresented = false } }
            
            HStack {
                VStack(alignment: .leading, spacing: 0) {
                    // Header
                    VStack(alignment: .leading, spacing: 12) {
                        Text(sessionManager.currentUser?.fullName ?? t("guest"))
                            .font(.system(size: 24, weight: .bold))
                            .foregroundColor(.white)
                        Text(sessionManager.currentUser?.email ?? "guest@local")
                            .font(.system(size: 14)).foregroundColor(.gray)
                        
                        // ВЫБОР ЯЗЫКА (КАК В АНДРОИД)
                        HStack(spacing: 15) {
                            LanguageButton(lang: "RU", isSelected: localizationManager.currentLanguage == "ru") { localizationManager.currentLanguage = "ru" }
                            LanguageButton(lang: "EN", isSelected: localizationManager.currentLanguage == "en") { localizationManager.currentLanguage = "en" }
                            LanguageButton(lang: "ZH", isSelected: localizationManager.currentLanguage == "zh") { localizationManager.currentLanguage = "zh" }
                        }
                        .padding(.top, 10)
                    }
                    .padding(30)
                    .padding(.top, 50)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(Color(hex: "0A0E1A"))
                    
                    // Menu Items
                    ScrollView {
                        VStack(alignment: .leading, spacing: 5) {
                            if !sessionManager.isGuest() {
                                DrawerMenuItem(icon: "person.fill", title: t("profile")) { navigate(.profile) }
                                DrawerMenuItem(icon: "doc.text.fill", title: t("admission")) { navigate(.admission) }
                                DrawerMenuItem(icon: "message.fill", title: t("support")) { navigate(.support) }
                            }
                            
                            DrawerMenuItem(icon: "newspaper.fill", title: t("news")) { navigate(.news) }
                            DrawerMenuItem(icon: "graduationcap.fill", title: t("universities")) { navigate(.universities) }
                            DrawerMenuItem(icon: "book.fill", title: t("programs")) { navigate(.programs) }
                            DrawerMenuItem(icon: "person.2.fill", title: t("partners")) { navigate(.partners) }
                            
                            Divider().background(Color.white.opacity(0.2)).padding(.vertical, 10)
                            DrawerMenuItem(icon: "arrow.right.square.fill", title: t("logout")) {
                                sessionManager.logout()
                                withAnimation { isPresented = false }
                            }
                        }
                        .padding(.top, 20)
                    }
                    .background(Color(hex: "0A0E1A"))
                }
                .frame(width: 300)
                .background(Color(hex: "0A0E1A"))
                .ignoresSafeArea()
                
                Spacer()
            }
        }
    }
    
    private func navigate(_ sheet: MainTabView.ActiveSheet) {
        withAnimation {
            isPresented = false
            onNavigate(sheet)
        }
    }
}

// Кнопка выбора языка
struct LanguageButton: View {
    let lang: String
    let isSelected: Bool
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            Text(lang)
                .font(.system(size: 14, weight: .bold))
                .foregroundColor(isSelected ? .black : .white)
                .frame(width: 40, height: 40)
                .background(isSelected ? Color.white : Color.white.opacity(0.1))
                .clipShape(Circle())
        }
    }
}

struct DrawerMenuItem: View {
    let icon: String
    let title: String
    let action: () -> Void
    var body: some View {
        Button(action: action) {
            HStack(spacing: 16) {
                Image(systemName: icon).foregroundColor(.white).frame(width: 24)
                Text(title).foregroundColor(.white).font(.system(size: 18))
                Spacer()
            }
            .padding(.horizontal, 30).padding(.vertical, 15)
            .contentShape(Rectangle())
        }
        .buttonStyle(PlainButtonStyle())
    }
}
