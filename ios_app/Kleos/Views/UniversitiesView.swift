import SwiftUI

struct UniversitiesView: View {
    @StateObject private var apiClient = ApiClient.shared
    @State private var universities: [University] = []
    @State private var isLoading = true
    
    var body: some View {
        NavigationView {
            ZStack {
                Color.kleosBackground.ignoresSafeArea()
                
                // Background circles
                VStack {
                    HStack {
                        Spacer()
                        BlurredCircle(color: Color.kleosBlue.opacity(0.2))
                            .offset(x: 50, y: -50)
                    }
                    Spacer()
                }
                
                if isLoading {
                    LoadingView()
                } else {
                    ScrollView {
                        VStack(alignment: .leading, spacing: 20) {
                            Text("Universities")
                                .font(.system(size: 32, weight: .bold))
                                .foregroundColor(.white)
                                .padding(.horizontal, 24)
                            
                            LazyVStack(spacing: 16) {
                                ForEach(universities) { uni in
                                    NavigationLink(destination: UniversityDetailView(universityId: uni.id)) {
                                        UniversityCard(university: uni)
                                    }
                                }
                            }
                            .padding(.horizontal, 24)
                        }
                        .padding(.top, 20)
                    }
                }
            }
            .navigationTitle("Universities")
            .navigationBarTitleDisplayMode(.large)
            .onAppear {
                loadUniversities()
            }
        }
    }
    
    private func loadUniversities() {
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
                }
            }
        }
    }
}

struct UniversityCard: View {
    let university: University
    
    var body: some View {
        ZStack(alignment: .bottomLeading) {
            // Background
            AsyncImage(url: ApiClient.shared.getFullUrl(university.logoUrl)) { phase in
                if case .success(let image) = phase {
                    image
                        .resizable()
                        .aspectRatio(contentMode: .fill)
                } else {
                    RoundedRectangle(cornerRadius: 20)
                        .fill(Color.white.opacity(0.1))
                }
            }
            .frame(height: 180)
            .clipped()
            
            // Overlay
            LinearGradient(
                gradient: Gradient(colors: [.black.opacity(0.7), .clear]),
                startPoint: .bottom,
                endPoint: .center
            )
            
            VStack(alignment: .leading, spacing: 4) {
                CategoryBadge(text: "Universities", isInteresting: false)
                
                Text(university.name)
                    .font(.system(size: 20, weight: .bold))
                    .foregroundColor(.white)
                
                HStack {
                    Image(systemName: "mappin.and.ellipse")
                    Text(university.location)
                }
                .font(.system(size: 14))
                .foregroundColor(.gray)
            }
            .padding(20)
            
            // Arrow button
            VStack {
                Spacer()
                HStack {
                    Spacer()
                    Image(systemName: "arrow.up.right.circle.fill")
                        .resizable()
                        .frame(width: 32, height: 32)
                        .foregroundColor(.white)
                        .padding(20)
                }
            }
        }
        .cornerRadius(20)
        .shadow(radius: 10)
    }
}

struct UniversityDetailView: View {
    let universityId: String
    @StateObject private var apiClient = ApiClient.shared
    @State private var university: University?
    @State private var isLoading = true
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
            
            if isLoading {
                LoadingView()
            } else if let university = university {
                ScrollView {
                    VStack(alignment: .leading, spacing: 24) {
                        // Logo
                        AsyncImage(url: apiClient.getFullUrl(university.logoUrl)) { phase in
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
                            Text(university.name)
                                .font(.system(size: 32, weight: .bold))
                                .foregroundColor(.white)
                            
                            HStack {
                                Image(systemName: "mappin.and.ellipse")
                                Text(university.location)
                            }
                            .font(.system(size: 16))
                            .foregroundColor(.gray)
                            
                            if let description = university.description {
                                Text(description)
                                    .font(.system(size: 16))
                                    .foregroundColor(.white)
                                    .lineSpacing(4)
                            }
                            
                            if let website = university.website, let url = URL(string: website) {
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
            loadUniversity()
        }
    }
    
    private func loadUniversity() {
        Task {
            do {
                let fetched = try await apiClient.fetchUniversity(id: universityId)
                await MainActor.run {
                    self.university = fetched
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

