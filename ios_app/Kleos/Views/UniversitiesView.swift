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
            } else if universities.isEmpty {
                VStack(spacing: 20) {
                    Image(systemName: "graduationcap")
                        .font(.system(size: 60))
                        .foregroundColor(.gray)
                    Text(t("no_content"))
                        .foregroundColor(.white)
                    Button(action: { loadUniversities() }) {
                        Text(t("reset_filters")) // Используем "Сбросить" как "Обновить"
                            .fontWeight(.semibold)
                    }
                    .buttonStyle(KleosButtonStyle(backgroundColor: .white.opacity(0.1), foregroundColor: .white))
                    .frame(width: 200)
                }
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
                let fetched = try await apiClient.getUniversities()
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
        HStack(spacing: 14) {
            AsyncImage(url: ApiClient.shared.getFullUrl(university.logoUrl)) { phase in
                if case .success(let image) = phase {
                    image
                        .resizable()
                        .scaledToFit()
                        .padding(10)
                        .background(Color.white)
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                } else {
                    ZStack {
                        RoundedRectangle(cornerRadius: 12).fill(Color.white.opacity(0.12))
                        Image(systemName: "graduationcap").foregroundColor(.white.opacity(0.6))
                    }
                }
            }
            .frame(width: 74, height: 74)

            VStack(alignment: .leading, spacing: 6) {
                Text(university.name)
                    .font(.system(size: 20, weight: .bold))
                    .foregroundColor(.white)
                    .lineLimit(2)
                HStack(spacing: 6) {
                    Image(systemName: "mappin.and.ellipse")
                    Text(university.location)
                }
                .font(.system(size: 14))
                .foregroundColor(.white.opacity(0.8))
            }
            Spacer()
            Image(systemName: "arrow.up.right")
                .font(.system(size: 14, weight: .bold))
                .foregroundColor(.black)
                .padding(8)
                .background(Color.white)
                .clipShape(Circle())
        }
        .padding(16)
        .background(Color.white.opacity(0.08))
        .overlay(RoundedRectangle(cornerRadius: 20).stroke(Color.white.opacity(0.1), lineWidth: 1))
        .frame(height: 120)
        .cornerRadius(20)
    }
}

struct UniversityDetailView: View {
    let universityId: String
    @StateObject private var apiClient = ApiClient.shared
    @StateObject private var localizationManager = LocalizationManager.shared
    @State private var university: University?
    @State private var isLoading = true
    @State private var showContacts = false
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
                                Text(university.city ?? "")
                            }
                            .font(.system(size: 16)).foregroundColor(.gray)
                        }
                        .padding(.horizontal)
                        
                        if let description = university.description, !description.isEmpty {
                            Text(description).font(.system(size: 16)).foregroundColor(.white.opacity(0.9)).lineSpacing(6).multilineTextAlignment(.center).padding(.horizontal, 24)
                        }
                        
                        Button(action: { showContacts = true }) {
                            Text(t("contacts"))
                                .font(.system(size: 24, weight: .bold))
                                .foregroundColor(.white)
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 16)
                                .background(Color.white.opacity(0.1))
                                .cornerRadius(16)
                        }
                        .padding(.horizontal, 24)
                        Color.clear.frame(height: 50)
                    }
                }
            }
        }
        .kleosBackground()
        .navigationBarTitleDisplayMode(.inline)
        .task { loadUniversity() }
        .onChange(of: localizationManager.currentLanguage) { _, _ in loadUniversity() }
        .sheet(isPresented: $showContacts) {
            contactsSheet
                .presentationDetents([.medium, .large])
        }
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
                let fetched = try await apiClient.getUniversity(id: universityId)
                await MainActor.run { self.university = fetched; self.isLoading = false }
            } catch {
                await MainActor.run { self.isLoading = false }
            }
        }
    }

    @ViewBuilder
    private var contactsSheet: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text(t("contacts"))
                .font(.system(size: 22, weight: .bold))
                .foregroundColor(.white)

            if let university = university {
                if let website = university.website, let url = URL(string: website.contains("://") ? website : "https://\(website)") {
                    Link(destination: url) { contactRow(icon: "globe", text: t("website")) }
                }
                if let email = university.socialLinks?.email, !email.isEmpty {
                    Link(destination: URL(string: "mailto:\(email)")!) { contactRow(icon: "envelope", text: email) }
                }
                if let phone = university.socialLinks?.phone, !phone.isEmpty {
                    Link(destination: URL(string: "tel:\(phone.filter { "0123456789+".contains($0) })")!) { contactRow(icon: "phone", text: phone) }
                }
                if let whatsapp = university.socialLinks?.whatsapp, !whatsapp.isEmpty {
                    Link(destination: URL(string: "https://wa.me/\(whatsapp.filter { "0123456789".contains($0) })")!) {
                        contactRow(icon: "message", text: "WhatsApp")
                    }
                }
                if (university.website == nil || university.website?.isEmpty == true)
                    && (university.socialLinks?.email == nil || university.socialLinks?.email?.isEmpty == true)
                    && (university.socialLinks?.phone == nil || university.socialLinks?.phone?.isEmpty == true)
                    && (university.socialLinks?.whatsapp == nil || university.socialLinks?.whatsapp?.isEmpty == true) {
                    Text("Contacts are not configured yet.")
                        .foregroundColor(.gray)
                }
            }

            Spacer()
        }
        .padding(20)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color.kleosBackground)
    }
}
