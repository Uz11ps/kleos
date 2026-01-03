import SwiftUI

struct GalleryView: View {
    @StateObject private var apiClient = ApiClient.shared
    @StateObject private var localizationManager = LocalizationManager.shared
    @State private var galleryItems: [GalleryItem] = []
    @State private var isLoading = false
    
    let columns = [
        GridItem(.flexible(), spacing: 16),
        GridItem(.flexible(), spacing: 16)
    ]
    
    var body: some View {
        ZStack {
            if isLoading {
                LoadingView()
            } else {
                ScrollView {
                    VStack(spacing: 0) {
                        Color.clear.frame(height: 100)
                        
                    LazyVGrid(columns: columns, spacing: 16) {
                        ForEach(galleryItems) { item in
                            NavigationLink(destination: GalleryDetailView(item: item)) {
                                GalleryCard(item: item)
                            }
                        }
                    }
                        .padding(.horizontal)
                    }
                    .padding(.bottom, 20)
                }
            }
        }
        .kleosBackground()
        .navigationTitle(t("gallery"))
        .navigationBarTitleDisplayMode(.large)
            .task {
            if galleryItems.isEmpty && !isLoading { loadGallery() }
        }
        .onChange(of: localizationManager.currentLanguage) { _, _ in loadGallery() }
    }
    
    private func loadGallery() {
        guard !isLoading else { return }
        isLoading = true
        Task {
            do {
                let fetched = try await apiClient.fetchGallery()
                await MainActor.run { self.galleryItems = fetched; self.isLoading = false }
            } catch {
                await MainActor.run { self.isLoading = false }
            }
        }
    }
}

struct GalleryCard: View {
    let item: GalleryItem
    var body: some View {
        ZStack(alignment: .bottomLeading) {
            AsyncImage(url: ApiClient.shared.getFullUrl(item.mediaUrl)) { phase in
                if case .success(let image) = phase {
                    image.resizable().aspectRatio(contentMode: .fill)
                } else {
                    Color.gray.opacity(0.3)
                }
            }
            .frame(height: 200).clipped()
            LinearGradient(gradient: Gradient(colors: [.black.opacity(0.7), .clear]), startPoint: .bottom, endPoint: .center)
            VStack(alignment: .leading, spacing: 4) {
                CategoryBadge(text: LocalizationManager.shared.t("gallery"), isInteresting: false)
                Text(item.title).font(.system(size: 16, weight: .bold)).foregroundColor(.white).lineLimit(2)
            }
            .padding(16)
        }
        .cornerRadius(20).shadow(radius: 10)
    }
}

struct GalleryDetailView: View {
    let item: GalleryItem
    @StateObject private var localizationManager = LocalizationManager.shared
    @Environment(\.dismiss) var dismiss
    
    var body: some View {
        ZStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 24) {
                    Color.clear.frame(height: 100)
                    if item.mediaType == "video" {
                        Text(t("video_player")).frame(height: 300).frame(maxWidth: .infinity).background(Color.gray.opacity(0.3))
                    } else {
                        AsyncImage(url: ApiClient.shared.getFullUrl(item.mediaUrl)) { phase in
                            if case .success(let image) = phase {
                                image.resizable().aspectRatio(contentMode: .fit)
                            } else {
                                Color.gray.opacity(0.3)
                            }
                        }
                        .frame(maxHeight: 500)
                    }
                    VStack(alignment: .leading, spacing: 16) {
                        CategoryBadge(text: t("gallery"), isInteresting: false)
                        Text(item.title).font(.system(size: 28, weight: .bold)).foregroundColor(.white)
                        if let description = item.description {
                            Text(description).font(.system(size: 16)).foregroundColor(.white).lineSpacing(4)
                        }
                    }
                    .padding()
                }
            }
        }
        .kleosBackground()
        .navigationBarTitleDisplayMode(.inline)
    }
}
