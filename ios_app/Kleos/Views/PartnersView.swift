import SwiftUI

struct PartnersView: View {
    @StateObject private var apiClient = ApiClient.shared
    @StateObject private var localizationManager = LocalizationManager.shared
    @State private var partners: [Partner] = []
    @State private var isLoading = false
    
    var body: some View {
        ZStack {
            if isLoading {
                LoadingView()
            } else {
                ScrollView {
                    VStack(alignment: .leading, spacing: 0) {
                        Color.clear.frame(height: 100)
                        
                        LazyVStack(spacing: 16) {
                            ForEach(partners) { partner in
                                NavigationLink(destination: PartnerDetailView(partner: partner)) {
                                    PartnerCard(partner: partner)
                                }
                            }
                        }
                        .padding(.horizontal, 24)
                        .padding(.bottom, 20)
                    }
                }
            }
        }
        .kleosBackground()
        .navigationTitle(t("partners"))
        .navigationBarTitleDisplayMode(.large)
        .task {
            if partners.isEmpty && !isLoading { loadPartners() }
        }
        .onChange(of: localizationManager.currentLanguage) { _, _ in loadPartners() }
    }
    
    private func loadPartners() {
        guard !isLoading else { return }
        isLoading = true
        Task {
            do {
                let fetched = try await apiClient.fetchPartners()
                await MainActor.run { self.partners = fetched; self.isLoading = false }
            } catch {
                await MainActor.run { self.isLoading = false }
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
                    image.resizable().aspectRatio(contentMode: .fill)
                } else {
                    RoundedRectangle(cornerRadius: 20).fill(Color.white.opacity(0.1))
                }
            }
            .frame(height: 200).clipped()
            LinearGradient(gradient: Gradient(colors: [.black.opacity(0.7), .clear]), startPoint: .bottom, endPoint: .center)
            VStack(alignment: .leading, spacing: 4) {
                CategoryBadge(text: LocalizationManager.shared.t("partners"), isInteresting: false)
                Text(partner.name).font(.system(size: 20, weight: .bold)).foregroundColor(.white)
            }
            .padding(20)
        }
        .cornerRadius(20).shadow(radius: 10)
    }
}

struct PartnerDetailView: View {
    let partner: Partner
    @StateObject private var localizationManager = LocalizationManager.shared
    @Environment(\.dismiss) var dismiss
    var body: some View {
        ZStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 24) {
                    Color.clear.frame(height: 100)
                    AsyncImage(url: ApiClient.shared.getFullUrl(partner.logoUrl)) { phase in
                        if case .success(let image) = phase {
                            image.resizable().aspectRatio(contentMode: .fit)
                        } else {
                            Color.gray.opacity(0.3)
                        }
                    }
                    .frame(height: 200).padding()
                    VStack(alignment: .leading, spacing: 16) {
                        CategoryBadge(text: t("partners"), isInteresting: false)
                        Text(partner.name).font(.system(size: 32, weight: .bold)).foregroundColor(.white)
                        if let description = partner.description {
                            Text(description).font(.system(size: 16)).foregroundColor(.white).lineSpacing(4)
                        }
                        if let website = partner.website, let url = URL(string: website) {
                            Link(destination: url) {
                                HStack { Text(t("open_website")); Image(systemName: "arrow.up.right") }
                                .font(.system(size: 16, weight: .semibold)).foregroundColor(.blue)
                            }
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
