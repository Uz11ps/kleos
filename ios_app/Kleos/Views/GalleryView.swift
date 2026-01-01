import SwiftUI

struct GalleryView: View {
    @StateObject private var apiClient = ApiClient.shared
    @State private var galleryItems: [GalleryItem] = []
    @State private var isLoading = true
    
    let columns = [
        GridItem(.flexible(), spacing: 16),
        GridItem(.flexible(), spacing: 16)
    ]
    
    var body: some View {
        NavigationView {
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
                
                if isLoading {
                    LoadingView()
                } else {
                    ScrollView {
                        LazyVGrid(columns: columns, spacing: 16) {
                            ForEach(galleryItems) { item in
                                NavigationLink(destination: GalleryDetailView(item: item)) {
                                    GalleryCard(item: item)
                                }
                            }
                        }
                        .padding()
                    }
                }
            }
            .navigationTitle("Gallery")
            .navigationBarTitleDisplayMode(.large)
            .onAppear {
                loadGallery()
            }
        }
    }
    
    private func loadGallery() {
        isLoading = true
        Task {
            do {
                print("🔄 Loading gallery...")
                let fetched = try await apiClient.fetchGallery()
                print("✅ Loaded \(fetched.count) gallery items")
                await MainActor.run {
                    self.galleryItems = fetched
                    self.isLoading = false
                }
            } catch {
                print("❌ Error loading gallery: \(error)")
                await MainActor.run {
                    self.isLoading = false
                    print("⚠️ Failed to load gallery: \(error.localizedDescription)")
                }
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
                    image
                        .resizable()
                        .aspectRatio(contentMode: .fill)
                } else {
                    Color.gray.opacity(0.3)
                }
            }
            .frame(height: 200)
            .clipped()
            
            LinearGradient(
                gradient: Gradient(colors: [.black.opacity(0.7), .clear]),
                startPoint: .bottom,
                endPoint: .center
            )
            
            VStack(alignment: .leading, spacing: 4) {
                CategoryBadge(text: "Gallery", isInteresting: false)
                
                Text(item.title)
                    .font(.system(size: 16, weight: .bold))
                    .foregroundColor(.white)
                    .lineLimit(2)
            }
            .padding(16)
        }
        .cornerRadius(20)
        .shadow(radius: 10)
    }
}

struct GalleryDetailView: View {
    let item: GalleryItem
    @Environment(\.dismiss) var dismiss
    
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
                    // Media
                    if item.mediaType == "video" {
                        // Video player placeholder
                        Text("Video Player")
                            .frame(height: 300)
                            .frame(maxWidth: .infinity)
                            .background(Color.gray.opacity(0.3))
                    } else {
                        AsyncImage(url: ApiClient.shared.getFullUrl(item.mediaUrl)) { phase in
                            if case .success(let image) = phase {
                                image
                                    .resizable()
                                    .aspectRatio(contentMode: .fit)
                            } else {
                                Color.gray.opacity(0.3)
                            }
                        }
                        .frame(maxHeight: 500)
                    }
                    
                    // Content
                    VStack(alignment: .leading, spacing: 16) {
                        CategoryBadge(text: "Gallery", isInteresting: false)
                        
                        Text(item.title)
                            .font(.system(size: 28, weight: .bold))
                            .foregroundColor(.white)
                        
                        if let description = item.description {
                            Text(description)
                                .font(.system(size: 16))
                                .foregroundColor(.white)
                                .lineSpacing(4)
                        }
                    }
                    .padding()
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
    }
}

