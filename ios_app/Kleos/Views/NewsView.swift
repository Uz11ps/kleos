import SwiftUI

struct NewsView: View {
    @StateObject private var apiClient = ApiClient.shared
    @State private var news: [NewsItem] = []
    @State private var isLoading = true
    @State private var selectedTab = "all"
    
    var body: some View {
        NavigationView {
            ZStack {
                Color.kleosBackground.ignoresSafeArea()
                
                VStack {
                    // Tabs
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
                    .padding()
                    
                    // News List
                    if isLoading {
                        LoadingView()
                    } else {
                        ScrollView {
                            LazyVStack(spacing: 16) {
                                ForEach(filteredNews) { item in
                                    NavigationLink(destination: NewsDetailView(newsId: item.id)) {
                                        NewsCard(item: item)
                                    }
                                }
                            }
                            .padding()
                        }
                    }
                }
            }
            .navigationTitle("News")
            .navigationBarTitleDisplayMode(.large)
            .onAppear {
                loadContent()
            }
        }
    }
    
    private var filteredNews: [NewsItem] {
        switch selectedTab {
        case "news":
            return news.filter { !($0.isInteresting ?? false) }
        case "interesting":
            return news.filter { $0.isInteresting ?? true }
        default:
            return news
        }
    }
    
    private func loadContent() {
        isLoading = true
        
        Task {
            do {
                print("🔄 Loading news...")
                let fetchedNews = try await apiClient.fetchNews()
                print("✅ Loaded \(fetchedNews.count) news items")
                await MainActor.run {
                    self.news = fetchedNews
                    self.isLoading = false
                }
            } catch {
                print("❌ Error loading news: \(error)")
                await MainActor.run {
                    self.isLoading = false
                    print("⚠️ Failed to load news: \(error.localizedDescription)")
                }
            }
        }
    }
}

struct NewsDetailView: View {
    let newsId: String
    @StateObject private var apiClient = ApiClient.shared
    @State private var news: NewsItem?
    @State private var isLoading = true
    @Environment(\.dismiss) var dismiss
    
    var body: some View {
        ZStack {
            Color.kleosBackground.ignoresSafeArea()
            
            if isLoading {
                LoadingView()
            } else if let news = news {
                ScrollView {
                    VStack(alignment: .leading, spacing: 20) {
                        // Image
                        AsyncImage(url: apiClient.getFullUrl(news.imageUrl)) { phase in
                            if case .success(let image) = phase {
                                image
                                    .resizable()
                                    .aspectRatio(contentMode: .fill)
                            } else {
                                Color.gray.opacity(0.3)
                            }
                        }
                        .frame(height: 300)
                        .clipped()
                        
                        // Content
                        VStack(alignment: .leading, spacing: 16) {
                            CategoryBadge(
                                text: (news.isInteresting ?? false) ? "Interesting" : "News",
                                isInteresting: news.isInteresting ?? false
                            )
                            
                            Text(news.title)
                                .font(.system(size: 28, weight: .bold))
                                .foregroundColor(.white)
                            
                            Text(news.dateText)
                                .font(.system(size: 14))
                                .foregroundColor(.gray)
                            
                            if let content = news.content {
                                Text(content)
                                    .font(.system(size: 16))
                                    .foregroundColor(.white)
                                    .lineSpacing(4)
                            }
                        }
                        .padding()
                    }
                }
            }
        }
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .navigationBarLeading) {
                Button(action: { dismiss() }) {
                    Image(systemName: "arrow.left")
                        .foregroundColor(.white)
                }
            }
        }
        .onAppear {
            loadNews()
        }
    }
    
    private func loadNews() {
        Task {
            do {
                let fetchedNews = try await apiClient.fetchNewsDetail(id: newsId)
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

