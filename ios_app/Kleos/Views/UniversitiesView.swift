import SwiftUI

struct UniversitiesView: View {
    @StateObject private var apiClient = ApiClient.shared
    @StateObject private var localizationManager = LocalizationManager.shared
    @State private var universities: [University] = []
    @State private var isLoading = false
    @State private var errorMessage: String? = nil
    
    var body: some View {
        ZStack {
            if isLoading {
                LoadingView()
            } else if let error = errorMessage, universities.isEmpty {
                VStack(spacing: 16) {
                    Text(t("error_loading_data"))
                        .foregroundColor(.gray)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal)
                    
                    Button(action: { loadUniversities() }) {
                        Text(t("retry"))
                            .fontWeight(.semibold)
                            .padding(.horizontal, 24)
                            .padding(.vertical, 10)
                            .background(Color.white.opacity(0.1))
                            .cornerRadius(20)
                    }
                }
                .frame(maxWidth: .infinity)
            } else {
                ScrollView {
                    VStack(alignment: .leading, spacing: 20) {
                        Color.clear.frame(height: 150)
                        
                        if universities.isEmpty {
                            Text(t("no_content"))
                                .foregroundColor(.gray)
                                .frame(maxWidth: .infinity)
                                .padding(.top, 40)
                        } else {
                            LazyVStack(spacing: 16) {
                                ForEach(universities) { uni in
                                    NavigationLink(destination: UniversityDetailView(universityId: uni.id)) {
                                        UniversityCard(university: uni)
                                    }
                                }
                            }
                            .padding(.horizontal, 24)
                        }
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
        errorMessage = nil
        Task {
            do {
                let fetched = try await apiClient.fetchUniversities()
                await MainActor.run {
                    self.universities = fetched
                    self.isLoading = false
                }
            } catch {
                await MainActor.run { 
                    self.isLoading = false 
                    self.errorMessage = error.localizedDescription
                }
            }
        }
    }
}

struct UniversityCard: View {
    let university: University
    var body: some View {
        ZStack(alignment: .bottomLeading) {
            AsyncImage(url: ApiClient.shared.getFullUrl(university.logoUrl)) { phase in
                if case .success(let image) = phase {
                    image.resizable().aspectRatio(contentMode: .fill)
                } else {
                    RoundedRectangle(cornerRadius: 20).fill(Color.white.opacity(0.1))
                }
            }
            .frame(height: 180).clipped()
            LinearGradient(gradient: Gradient(colors: [.black.opacity(0.7), .clear]), startPoint: .bottom, endPoint: .center)
            VStack(alignment: .leading, spacing: 4) {
                CategoryBadge(text: LocalizationManager.shared.t("university"), isInteresting: false)
                Text(university.name).font(.system(size: 20, weight: .bold)).foregroundColor(.white)
                HStack {
                    Image(systemName: "mappin.and.ellipse")
                    Text(university.location)
                }
                .font(.system(size: 14)).foregroundColor(.gray)
            }
            .padding(20)
            VStack {
                Spacer()
                HStack {
                    Spacer()
                    Image(systemName: "arrow.up.right.circle.fill").resizable().frame(width: 32, height: 32).foregroundColor(.white).padding(20)
                }
            }
        }
        .cornerRadius(20).shadow(radius: 10)
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
                                Text(university.location)
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
                let fetched = try await apiClient.fetchUniversity(id: universityId)
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
