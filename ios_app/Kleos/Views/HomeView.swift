import SwiftUI

struct HomeView: View {
    @StateObject private var apiClient = ApiClient.shared
    @StateObject private var sessionManager = SessionManager.shared
    @StateObject private var localizationManager = LocalizationManager.shared
    
    @State private var news: [NewsItem] = []
    @State private var isLoading = false
    @State private var hasLoadedOnce = false
    @State private var selectedTab = "all"
    @State private var userProfile: UserProfile?
    @State private var errorMessage: String?
    @State private var loadingTask: Task<Void, Never>?
    
    @Environment(\.presentationMode) var presentationMode
    
    var body: some View {
        ZStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 24) {
                    Color.clear.frame(height: 90)
                    
                    headerView
                    welcomeSection
                    tabsView
                    newsSection
                }
            }
            .refreshable {
                loadData()
            }
        }
        .kleosBackground()
        .task {
            // Загружаем данные только если не гость или если есть токен
            if !hasLoadedOnce && !isLoading {
                loadData()
            }
        }
        .onChange(of: localizationManager.currentLanguage) { _, _ in
            loadData()
        }
        .onReceive(sessionManager.$isUserGuest) { isGuest in
            // Перезагружаем при смене статуса (из гостя в юзера)
            if !isGuest {
                loadData()
            }
        }
    }
    
    private var headerView: some View {
        HStack {
            NavigationLink(destination: ProfileView()) {
                AsyncImage(url: apiClient.getFullUrl(userProfile?.avatarUrl)) { phase in
                    if case .success(let image) = phase {
                        image.resizable().aspectRatio(contentMode: .fill)
                    } else {
                        Image(systemName: "person.circle.fill").resizable().foregroundColor(Color.kleosPurple)
                    }
                }
                .frame(width: 40, height: 40).clipShape(Circle())
            }
            Spacer()
        }
        .padding(.horizontal, 24)
    }
    
    private var welcomeSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(t("welcome_back"))
                .font(.system(size: 16)).foregroundColor(.gray)
            
            let userName = userProfile?.fullName ?? sessionManager.currentUser?.fullName ?? t("guest")
            Text("\(userName)")
                .font(.system(size: 32, weight: .bold)).foregroundColor(.white).lineSpacing(4)
            
            Text(t("explore_new_areas"))
                .font(.system(size: 32, weight: .bold)).foregroundColor(.white).lineSpacing(4).padding(.top, 8)
        }
        .padding(.horizontal, 24)
    }
    
    private var tabsView: some View {
        HStack(spacing: 12) {
            TabButton(title: t("all"), isSelected: selectedTab == "all") {
                selectedTab = "all"
                loadContent()
            }
            TabButton(title: t("news"), isSelected: selectedTab == "news") {
                selectedTab = "news"
                loadContent()
            }
            TabButton(title: t("interesting"), isSelected: selectedTab == "interesting") {
                selectedTab = "interesting"
                loadContent()
            }
        }
        .padding(.horizontal, 24)
    }
    
    private var newsSection: some View {
        VStack(alignment: .leading, spacing: 16) {
            if isLoading {
                LoadingView().frame(height: 200)
            } else {
                let filteredNews = filteredNewsItems
                if filteredNews.isEmpty {
                    Text(t("no_content")).foregroundColor(.gray).padding().frame(maxWidth: .infinity)
                } else {
                    VStack(spacing: 16) {
                        ForEach(filteredNews) { item in
                            NavigationLink(destination: NewsDetailView(newsId: item.id)) {
                                NewsCard(item: item)
                            }
                        }
                    }
                    .padding(.horizontal, 24)
                }
            }
        }
    }
    
    private var filteredNewsItems: [NewsItem] {
        switch selectedTab {
        case "news": return news.filter { !($0.isInteresting ?? false) }
        case "interesting": return news.filter { $0.isInteresting == true }
        default: return news
        }
    }
    
    private func loadData() {
        loadUserProfile()
        loadContent()
    }
    
    private func loadUserProfile() {
        // Если гость - не запрашиваем профиль с сервера
        if sessionManager.isGuest() {
            print("👤 HomeView: Guest mode, using local data")
            self.userProfile = sessionManager.currentUser
            return
        }
        
        Task {
            do {
                // Ждем немного, чтобы токен "прописался" в системе
                try await Task.sleep(nanoseconds: 300_000_000)
                let profile = try await apiClient.getProfile()
                await MainActor.run { self.userProfile = profile }
            } catch {
                print("⚠️ HomeView: Profile load failed, using local")
                await MainActor.run {
                    if let currentUser = sessionManager.currentUser {
                        self.userProfile = currentUser
                    }
                }
            }
        }
    }
    
    private func loadContent() {
        loadingTask?.cancel()
        guard !isLoading else { return }
        isLoading = true
        loadingTask = Task {
            do {
                let fetchedNews = try await apiClient.fetchNews()
                guard !Task.isCancelled else { return }
                await MainActor.run {
                    self.news = fetchedNews
                    self.isLoading = false
                    self.hasLoadedOnce = true
                }
            } catch {
                if error is CancellationError { return }
                await MainActor.run {
                    self.isLoading = false
                    self.hasLoadedOnce = true
                    self.errorMessage = error.localizedDescription
                }
            }
        }
    }
}

struct TabButton: View {
    let title: String
    let isSelected: Bool
    let action: () -> Void
    var body: some View {
        Button(action: action) {
            Text(title)
                .font(.system(size: 14, weight: isSelected ? .semibold : .regular))
                .foregroundColor(isSelected ? .black : .white)
                .padding(.horizontal, 16).padding(.vertical, 8)
                .background(isSelected ? Color.white : Color.white.opacity(0.26))
                .cornerRadius(20)
        }
    }
}

struct NewsCard: View {
    let item: NewsItem
    var isInteresting: Bool { item.isInteresting ?? false }
    var backgroundColor: Color { isInteresting ? Color(hex: "FFD700") : Color(hex: "E8D5FF") }
    var body: some View {
        ZStack(alignment: .topLeading) {
            backgroundColor.frame(width: UIScreen.main.bounds.width - 48, height: 200)
            if let imageUrl = item.imageUrl, !imageUrl.isEmpty {
                AsyncImage(url: ApiClient.shared.getFullUrl(imageUrl)) { phase in
                    if case .success(let image) = phase {
                        image.resizable().aspectRatio(contentMode: .fill)
                            .frame(width: UIScreen.main.bounds.width - 48, height: 200).clipped()
                    }
                }
            }
            VStack {
                HStack {
                    Spacer()
                    Image(systemName: "arrow.up.right").font(.system(size: 16, weight: .bold)).foregroundColor(.black)
                        .frame(width: 32, height: 32).background(Color.white).clipShape(Circle())
                        .padding(.top, 16).padding(.trailing, 16)
                }
                Spacer()
            }
            VStack(alignment: .leading, spacing: 0) {
                Spacer()
                CategoryBadge(text: isInteresting ? LocalizationManager.shared.t("interesting") : LocalizationManager.shared.t("news"), isInteresting: isInteresting)
                    .padding(.leading, 20).padding(.bottom, 8)
                Text(item.title).font(.system(size: 20, weight: .bold)).foregroundColor(isInteresting ? .black : .white)
                    .padding(.horizontal, 20).padding(.bottom, 4)
                Text(item.dateText).font(.system(size: 14)).foregroundColor(isInteresting ? Color.gray.opacity(0.8) : Color.white.opacity(0.8))
                    .padding(.leading, 20).padding(.bottom, 20)
            }
        }
        .frame(width: UIScreen.main.bounds.width - 48, height: 200).cornerRadius(20)
    }
}
