import SwiftUI

struct UniversitiesView: View {
    @StateObject private var apiClient = ApiClient.shared
    @StateObject private var localizationManager = LocalizationManager.shared
    @State private var universities: [University] = []
    @State private var isLoading = false
    
    var body: some View {
        ZStack {
            if isLoading {
                LoadingView()
            } else {
                ScrollView {
                    VStack(alignment: .leading, spacing: 20) {
                        Color.clear.frame(height: 150)
                        
                        LazyVStack(spacing: 16) {
                            ForEach(universities) { uni in
                                NavigationLink(destination: UniversityDetailView(universityId: uni.id)) {
                                    UniversityCard(university: uni)
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
        .navigationTitle(t("universities"))
        .navigationBarTitleDisplayMode(.large)
        .task {
            if universities.isEmpty && !isLoading {
                loadUniversities()
            }
        }
        .onChange(of: localizationManager.currentLanguage) { _, _ in
            loadUniversities()
        }
    }
    
    private func loadUniversities() {
        guard !isLoading else { return }
        isLoading = true
        Task {
            do {
                let fetched = try await apiClient.fetchUniversities()
                await MainActor.run {
                    self.universities = fetched
                    self.isLoading = false
                }
            } catch {
                await MainActor.run { self.isLoading = false }
            }
        }
    }
}

struct UniversityCard: View {
    let university: University
    var body: some View {
        ZStack(alignment: .bottomLeading) {
            // Background Image
            AsyncImage(url: ApiClient.shared.getFullUrl(university.logoUrl)) { phase in
                if case .success(let image) = phase {
                    image.resizable().aspectRatio(contentMode: .fill)
                } else {
                    Color(hex: "7E5074").opacity(0.3)
                }
            }
            .frame(height: 200).clipped()
            
            // Overlay darkening
            Color.black.opacity(0.4)
            
            // Arrow button
            VStack {
                HStack {
                    Spacer()
                    Image(systemName: "arrow.up.right")
                        .font(.system(size: 14, weight: .bold))
                        .foregroundColor(.black)
                        .padding(8)
                        .background(Color.white)
                        .clipShape(Circle())
                        .padding(16)
                }
                Spacer()
            }
            
            // Text content
            VStack(alignment: .leading, spacing: 4) {
                CategoryBadge(text: LocalizationManager.shared.t("universities"), isInteresting: false)
                    .padding(.bottom, 4)
                Text(university.name)
                    .font(.system(size: 20, weight: .bold))
                    .foregroundColor(.white)
                HStack {
                    Image(systemName: "mappin.and.ellipse")
                    Text(university.location)
                }
                .font(.system(size: 14)).foregroundColor(.white.opacity(0.8))
            }
            .padding(20)
        }
        .frame(height: 200)
        .cornerRadius(20)
    }
}

struct UniversityDetailView: View {
    let universityId: String
    @StateObject private var apiClient = ApiClient.shared
    @StateObject private var localizationManager = LocalizationManager.shared
    @State private var university: University?
    @State private var isLoading = true
    @Environment(\.dismiss) var dismiss
    
    var body: some View {
        ZStack {
            if isLoading {
                LoadingView()
            } else if let university = university {
                ScrollView {
                    VStack(alignment: .center, spacing: 24) {
                        Color.clear.frame(height: 150)
                        
                        AsyncImage(url: apiClient.getFullUrl(university.logoUrl)) { phase in
                            if case .success(let image) = phase {
                                image.resizable().aspectRatio(contentMode: .fit)
                            } else {
                                Color.white.opacity(0.1).overlay(Image(systemName: "graduationcap").foregroundColor(.gray))
                            }
                        }
                        .frame(height: 160).cornerRadius(12).padding(.horizontal)
                        
                        VStack(alignment: .center, spacing: 12) {
                            Text(university.name).font(.system(size: 28, weight: .bold)).foregroundColor(.white).multilineTextAlignment(.center)
                            HStack {
                                Image(systemName: "mappin.and.ellipse")
                                Text(university.location)
                            }
                            .font(.system(size: 16)).foregroundColor(.gray)
                        }
                        .padding(.horizontal)
                        
                        if let description = university.description, !description.isEmpty {
                            Text(description).font(.system(size: 16)).foregroundColor(.white.opacity(0.9)).lineSpacing(6).multilineTextAlignment(.center).padding(.horizontal, 24)
                        }
                        
                        if let socialLinks = university.socialLinks {
                            VStack(alignment: .leading, spacing: 16) {
                                Text(t("contacts")).font(.system(size: 20, weight: .bold)).foregroundColor(.white).padding(.bottom, 4)
                                if let website = university.website, let url = URL(string: website.contains("://") ? website : "https://\(website)") {
                                    Link(destination: url) { contactRow(icon: "globe", text: t("website")) }
                                }
                                if let email = socialLinks.email {
                                    Link(destination: URL(string: "mailto:\(email)")!) { contactRow(icon: "envelope", text: email) }
                                }
                                if let phone = socialLinks.phone {
                                    Link(destination: URL(string: "tel:\(phone.filter { "0123456789+".contains($0) })")!) { contactRow(icon: "phone", text: phone) }
                                }
                            }
                            .padding(20).background(Color.white.opacity(0.08)).cornerRadius(20).padding(.horizontal, 24)
                        }
                        Color.clear.frame(height: 50)
                    }
                }
            }
        }
        .kleosBackground()
        .navigationBarTitleDisplayMode(.inline)
        .task { loadUniversity() }
        .onChange(of: localizationManager.currentLanguage) { _, _ in loadUniversity() }
    }
    
    @ViewBuilder
    private func contactRow(icon: String, text: String) -> some View {
        HStack(spacing: 12) {
            Image(systemName: icon).foregroundColor(Color.kleosPurple).frame(width: 24)
            Text(text).font(.system(size: 16)).foregroundColor(.white)
            Spacer()
            Image(systemName: "arrow.up.right").font(.system(size: 12)).foregroundColor(.gray)
        }
        .padding(.vertical, 8).contentShape(Rectangle())
    }
    
    private func loadUniversity() {
        Task {
            do {
                let fetched = try await apiClient.fetchUniversity(id: universityId)
                await MainActor.run { self.university = fetched; self.isLoading = false }
            } catch {
                await MainActor.run { self.isLoading = false }
            }
        }
    }
}
