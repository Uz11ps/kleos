import SwiftUI

struct HomeView: View {
    @StateObject private var apiClient = ApiClient.shared
    @StateObject private var sessionManager = SessionManager.shared
    
    @State private var news: [NewsItem] = []
    @State private var isLoading = true
    @State private var selectedTab = "all" // all, news, interesting
    @State private var userProfile: UserProfile?
    
    var body: some View {
        ZStack {
            Color.kleosBackground.ignoresSafeArea()
            
            // Background circles
            VStack {
                HStack {
                    BlurredCircle()
                        .offset(x: -100, y: -100)
                    Spacer()
                }
                Spacer()
                HStack {
                    Spacer()
                    BlurredCircle(color: Color.kleosBlue.opacity(0.3))
                        .offset(x: 100, y: 100)
                }
            }
            .ignoresSafeArea()
            
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
        }
        .onAppear {
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
            if isLoading {
                LoadingView()
                    .frame(height: 400)
            } else {
                let filteredNews = filteredNewsItems
                
                if filteredNews.isEmpty {
                    Text("No content available")
                        .foregroundColor(.gray)
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
            return news.filter { !($0.isInteresting ?? false) }
        case "interesting":
            return news.filter { $0.isInteresting ?? true }
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
        isLoading = true
        
        Task {
            do {
                let fetchedNews = try await apiClient.fetchNews()
                await MainActor.run {
                    self.news = fetchedNews
                    self.isLoading = false
                }
            } catch {
                await MainActor.run {
                    self.isLoading = false
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
