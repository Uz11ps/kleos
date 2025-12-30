import SwiftUI

struct PartnersView: View {
    @StateObject private var apiClient = ApiClient.shared
    @State private var partners: [Partner] = []
    @State private var isLoading = true
    
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
                        LazyVStack(spacing: 16) {
                            ForEach(partners) { partner in
                                NavigationLink(destination: PartnerDetailView(partner: partner)) {
                                    PartnerCard(partner: partner)
                                }
                            }
                        }
                        .padding()
                    }
                }
            }
            .navigationTitle("Partners")
            .navigationBarTitleDisplayMode(.large)
            .onAppear {
                loadPartners()
            }
        }
    }
    
    private func loadPartners() {
        Task {
            do {
                let fetched = try await apiClient.fetchPartners()
                await MainActor.run {
                    self.partners = fetched
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

struct PartnerCard: View {
    let partner: Partner
    
    var body: some View {
        ZStack(alignment: .bottomLeading) {
            AsyncImage(url: ApiClient.shared.getFullUrl(partner.logoUrl)) { phase in
                if case .success(let image) = phase {
                    image
                        .resizable()
                        .aspectRatio(contentMode: .fill)
                } else {
                    RoundedRectangle(cornerRadius: 20)
                        .fill(Color.white.opacity(0.1))
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
                CategoryBadge(text: "Partners", isInteresting: false)
                
                Text(partner.name)
                    .font(.system(size: 20, weight: .bold))
                    .foregroundColor(.white)
            }
            .padding(20)
        }
        .cornerRadius(20)
        .shadow(radius: 10)
    }
}

struct PartnerDetailView: View {
    let partner: Partner
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
                    // Logo
                    AsyncImage(url: ApiClient.shared.getFullUrl(partner.logoUrl)) { phase in
                        if case .success(let image) = phase {
                            image
                                .resizable()
                                .aspectRatio(contentMode: .fit)
                        } else {
                            Color.gray.opacity(0.3)
                        }
                    }
                    .frame(height: 200)
                    .padding()
                    
                    // Content
                    VStack(alignment: .leading, spacing: 16) {
                        CategoryBadge(text: "Partners", isInteresting: false)
                        
                        Text(partner.name)
                            .font(.system(size: 32, weight: .bold))
                            .foregroundColor(.white)
                        
                        if let description = partner.description {
                            Text(description)
                                .font(.system(size: 16))
                                .foregroundColor(.white)
                                .lineSpacing(4)
                        }
                        
                        if let website = partner.website, let url = URL(string: website) {
                            Link(destination: url) {
                                HStack {
                                    Text("Open website")
                                    Image(systemName: "arrow.up.right")
                                }
                                .font(.system(size: 16, weight: .semibold))
                                .foregroundColor(.blue)
                            }
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

