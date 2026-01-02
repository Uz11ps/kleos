import SwiftUI

struct HomeView: View {
    @StateObject private var apiClient = ApiClient.shared
    @StateObject private var sessionManager = SessionManager.shared
    
    @State private var news: [NewsItem] = []
    @State private var isLoading = true
    @State private var selectedTab = "all" // all, news, interesting
    @State private var userProfile: UserProfile?
    @State private var errorMessage: String?
    @State private var debugInfo: String = ""
    
    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 24) {
                // Header
                headerView
                
                // Welcome Section
                welcomeSection
                
                // Tabs
                tabsView
                
                // News Section
                newsSection
            }
            .padding(.top, 20)
        }
        .kleosBackground() // Используем централизованный фон
        .onAppear {
            print("🏠 HomeView appeared, loading data...")
            print("🏠 Current news count: \(news.count)")
            print("🏠 isLoading: \(isLoading)")
            loadData()
        }

        .refreshable {
            print("🔄 HomeView refresh triggered")
            loadData()
        }
        .task {
            // Альтернативный способ загрузки через task
            print("📋 HomeView task started")
            loadData()
        }
    }
    
    private var headerView: some View {
        HStack {
            Button(action: {
                // Open drawer menu
            }) {
                Image(systemName: "line.3.horizontal")
                    .font(.title2)
                    .foregroundColor(.white)
            }
            
            Spacer()
            
            Button(action: {
                // Navigate to profile
            }) {
                AsyncImage(url: apiClient.getFullUrl(userProfile?.avatarUrl)) { phase in
                    if case .success(let image) = phase {
                        image
                            .resizable()
                            .aspectRatio(contentMode: .fill)
                    } else {
                        Image(systemName: "person.circle.fill")
                            .resizable()
                    }
                }
                .frame(width: 40, height: 40)
                .clipShape(Circle())
                .foregroundColor(.gray)
            }
        }
        .padding(.horizontal, 24)
    }
    
    private var welcomeSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Welcome back,")
                .font(.system(size: 16))
                .foregroundColor(.gray)
            
            let userName = userProfile?.fullName ?? sessionManager.currentUser?.fullName ?? "Guest"
            Text("\(userName)")
                .font(.system(size: 32, weight: .bold))
                .foregroundColor(.white)
                .lineSpacing(4)
        }
        .padding(.horizontal, 24)
    }
    
    private var tabsView: some View {
        HStack(spacing: 12) {
            TabButton(title: "All", isSelected: selectedTab == "all") {
                selectedTab = "all"
                loadContent()
            }
            
            TabButton(title: "News", isSelected: selectedTab == "news") {
                selectedTab = "news"
                loadContent()
            }
            
            TabButton(title: "Interesting", isSelected: selectedTab == "interesting") {
                selectedTab = "interesting"
                loadContent()
            }
        }
        .padding(.horizontal, 24)
    }
    
    private var newsSection: some View {
        VStack(alignment: .leading, spacing: 16) {
            // Debug info
            VStack(alignment: .leading, spacing: 4) {
                Text("Debug: isLoading=\(isLoading ? "true" : "false"), news.count=\(news.count), selectedTab=\(selectedTab)")
                    .font(.caption2)
                    .foregroundColor(.yellow)
                if let error = errorMessage {
                    Text("Error: \(error)")
                        .font(.caption)
                        .foregroundColor(.red)
                }
                if !debugInfo.isEmpty {
                    Text("Info: \(debugInfo)")
                        .font(.caption2)
                        .foregroundColor(.gray)
                }
            }
            .padding(.horizontal, 24)
            
            if isLoading {
                VStack {
                    LoadingView()
                        .frame(height: 400)
                    Text("Loading...")
                        .foregroundColor(.gray)
                        .padding()
                }
            } else {
                let filteredNews = filteredNewsItems
                
                if filteredNews.isEmpty {
                    VStack(spacing: 12) {
                        Text("No content available")
                            .foregroundColor(.gray)
                        Text("Total news: \(news.count)")
                            .font(.caption)
                            .foregroundColor(.gray)
                        Text("Filtered news: \(filteredNews.count)")
                            .font(.caption)
                            .foregroundColor(.gray)
                        Text("Selected tab: \(selectedTab)")
                            .font(.caption)
                            .foregroundColor(.gray)
                        if let error = errorMessage {
                            Text("Error: \(error)")
                                .font(.caption)
                                .foregroundColor(.red)
                                .padding()
                        }
                        if !debugInfo.isEmpty {
                            Text(debugInfo)
                                .font(.caption2)
                                .foregroundColor(.gray)
                                .padding()
                        }
                        Button("Retry") {
                            loadContent()
                        }
                        .buttonStyle(KleosButtonStyle())
                        .padding(.top, 8)
                    }
                    .frame(maxWidth: .infinity, alignment: .center)
                    .padding()
                } else {
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 16) {
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
    }
    
    private var filteredNewsItems: [NewsItem] {
        switch selectedTab {
        case "news":
            // Если isInteresting не задан, считаем все новостями
            return news.filter { !($0.isInteresting ?? false) }
        case "interesting":
            // Если isInteresting не задан, не показываем в интересном
            return news.filter { $0.isInteresting == true }
        default:
            return news
        }
    }
    
    private func loadData() {
        loadUserProfile()
        loadContent()
    }
    
    private func loadUserProfile() {
        Task {
            do {
                let profile = try await apiClient.getProfile()
                await MainActor.run {
                    self.userProfile = profile
                    sessionManager.saveUser(
                        fullName: profile.fullName,
                        email: profile.email,
                        role: profile.role
                    )
                }
            } catch {
                // Guest or error - use session data
                if let currentUser = sessionManager.currentUser {
                    self.userProfile = currentUser
                }
            }
        }
    }
    
    private func loadContent() {
        print("🔄 loadContent() called")
        print("🔄 Current state - isLoading: \(isLoading), news count: \(news.count)")
        
        isLoading = true
        errorMessage = nil
        debugInfo = "Loading..."
        
        Task { @MainActor in
            do {
                print("🔄 Task started, calling fetchNews()...")
                let fetchedNews = try await apiClient.fetchNews()
                print("✅ fetchNews() returned \(fetchedNews.count) items")
                
                self.news = fetchedNews
                self.isLoading = false
                self.debugInfo = "Loaded \(fetchedNews.count) items"
                self.errorMessage = nil
                
                print("✅ State updated - news count: \(self.news.count), isLoading: \(self.isLoading)")
            } catch {
                print("❌ Error in loadContent: \(error)")
                print("❌ Error type: \(type(of: error))")
                print("❌ Error description: \(error.localizedDescription)")
                
                self.isLoading = false
                self.errorMessage = error.localizedDescription
                self.debugInfo = "Error: \(error.localizedDescription)"
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
                .padding(.horizontal, 16)
                .padding(.vertical, 8)
                .background(isSelected ? Color.white : Color.white.opacity(0.26))
                .cornerRadius(20)
        }
    }
}

struct NewsCard: View {
    let item: NewsItem
    
    var body: some View {
        ZStack(alignment: .bottomLeading) {
            // Background image
            AsyncImage(url: ApiClient.shared.getFullUrl(item.imageUrl)) { phase in
                switch phase {
                case .success(let image):
                    image
                        .resizable()
                        .aspectRatio(contentMode: .fill)
                default:
                    Color.gray.opacity(0.3)
                }
            }
            .frame(width: 280, height: 380)
            .clipped()
            
            // Overlay gradient
            LinearGradient(
                gradient: Gradient(colors: [.black.opacity(0.7), .clear]),
                startPoint: .bottom,
                endPoint: .center
            )
            
            VStack(alignment: .leading, spacing: 8) {
                // Category badge
                CategoryBadge(
                    text: (item.isInteresting ?? false) ? "Interesting" : "News",
                    isInteresting: item.isInteresting ?? false
                )
                
                Text(item.title)
                    .font(.system(size: 18, weight: .bold))
                    .foregroundColor(.white)
                    .lineLimit(2)
                
                Text(item.dateText)
                    .font(.system(size: 14))
                    .foregroundColor(.gray)
            }
            .padding(20)
        }
        .frame(width: 280, height: 380)
        .cornerRadius(24)
        .shadow(radius: 10)
    }
}
