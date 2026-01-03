import SwiftUI

struct NewsView: View {
    @StateObject private var apiClient = ApiClient.shared
    @StateObject private var localizationManager = LocalizationManager.shared
    @State private var news: [NewsItem] = []
    @State private var isLoading = false
    @State private var selectedTab = "all"
    
    var body: some View {
        ZStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    Color.clear.frame(height: 150)
                    
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
                    .padding(.horizontal, 24).padding(.bottom, 24)
                    
                    if isLoading {
                        LoadingView().frame(height: 400)
                    } else {
                        let filtered = filteredNews
                        if filtered.isEmpty {
                            Text(t("no_news")).foregroundColor(.gray).padding().frame(maxWidth: .infinity)
                        } else {
                            LazyVStack(spacing: 16) {
                                ForEach(filtered) { item in
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
            .refreshable { loadContent() }
        }
        .kleosBackground()
        .navigationTitle(t("news"))
        .navigationBarTitleDisplayMode(.large)
        .task {
            if news.isEmpty && !isLoading { loadContent() }
        }
        .onChange(of: localizationManager.currentLanguage) { _, _ in loadContent() }
    }
    
    private var filteredNews: [NewsItem] {
        switch selectedTab {
        case "news": return news.filter { !($0.isInteresting ?? false) }
        case "interesting": return news.filter { $0.isInteresting == true }
        default: return news
        }
    }
    
    private func loadContent() {
        guard !isLoading else { return }
        isLoading = true
        Task {
            do {
                let fetchedNews = try await apiClient.fetchNews()
                await MainActor.run { self.news = fetchedNews; self.isLoading = false }
            } catch {
                await MainActor.run { self.isLoading = false }
            }
        }
    }
}

struct NewsDetailView: View {
    let newsId: String
    @StateObject private var apiClient = ApiClient.shared
    @StateObject private var localizationManager = LocalizationManager.shared
    @State private var news: NewsItem?
    @State private var isLoading = true
    @Environment(\.dismiss) var dismiss
    
    var body: some View {
        ZStack {
            if isLoading {
                LoadingView()
            } else if let news = news {
                ScrollView {
                    VStack(alignment: .leading, spacing: 20) {
                        Color.clear.frame(height: 150)
                        AsyncImage(url: apiClient.getFullUrl(news.imageUrl)) { phase in
                            if case .success(let image) = phase {
                                image.resizable().aspectRatio(contentMode: .fill)
                            } else {
                                Color.gray.opacity(0.3)
                            }
                        }
                        .frame(height: 300).clipped()
                        
                        VStack(alignment: .leading, spacing: 16) {
                            CategoryBadge(
                                text: (news.isInteresting ?? false) ? t("interesting") : t("news"),
                                isInteresting: news.isInteresting ?? false
                            )
                            Text(news.title).font(.system(size: 28, weight: .bold)).foregroundColor(.white)
                            Text(news.dateText).font(.system(size: 14)).foregroundColor(.gray)
                            if let content = news.content {
                                Text(content).font(.system(size: 16)).foregroundColor(.white).lineSpacing(4)
                            }
                        }
                        .padding()
                    }
                }
            }
        }
        .kleosBackground()
        .navigationBarTitleDisplayMode(.inline)
        .task { loadNews() }
        .onChange(of: localizationManager.currentLanguage) { _, _ in loadNews() }
    }
    
    private func loadNews() {
        Task {
            do {
                let fetchedNews = try await apiClient.fetchNewsDetail(id: newsId)
                await MainActor.run { self.news = fetchedNews; self.isLoading = false }
            } catch {
                await MainActor.run { self.isLoading = false }
            }
        }
    }
}
