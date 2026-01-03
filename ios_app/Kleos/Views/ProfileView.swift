import SwiftUI

struct ProfileView: View {
    @StateObject private var apiClient = ApiClient.shared
    @StateObject private var sessionManager = SessionManager.shared
    @State private var profile: UserProfile?
    @State private var isLoading = true
    @State private var errorMessage: String?
    
    var body: some View {
        ZStack {
            if isLoading {
                LoadingView()
            } else {
                ScrollView {
                    VStack(spacing: 24) {
                        // Avatar
                        AsyncImage(url: apiClient.getFullUrl(profile?.avatarUrl)) { phase in
                            if case .success(let image) = phase {
                                image
                                    .resizable()
                                    .aspectRatio(contentMode: .fill)
                            } else {
                                Image(systemName: "person.circle.fill")
                                    .resizable()
                                    .foregroundColor(.gray)
                            }
                        }
                        .frame(width: 100, height: 100)
                        .clipShape(Circle())
                        
                        // Error message
                        if let error = errorMessage {
                            Text("Error: \(error)")
                                .foregroundColor(.red)
                                .font(.caption)
                                .padding()
                        }
                        
                        // Profile fields
                        VStack(spacing: 16) {
                            ProfileField(label: "Full Name", value: profile?.fullName ?? sessionManager.currentUser?.fullName ?? "")
                            ProfileField(label: "Email", value: profile?.email ?? sessionManager.currentUser?.email ?? "")
                            ProfileField(label: "Role", value: profile?.role ?? sessionManager.currentUser?.role ?? "")
                            ProfileField(label: "Student ID", value: profile?.studentId ?? "")
                            ProfileField(label: "Phone", value: profile?.phone ?? "")
                            ProfileField(label: "Course", value: profile?.course ?? "")
                            ProfileField(label: "Speciality", value: profile?.speciality ?? "")
                            ProfileField(label: "Status", value: profile?.status ?? "")
                            ProfileField(label: "University", value: profile?.university ?? "")
                            ProfileField(label: "Payment", value: profile?.payment ?? "")
                            ProfileField(label: "Penalties", value: profile?.penalties ?? "")
                            ProfileField(label: "Notes", value: profile?.notes ?? "", isMultiline: true)
                        }
                        .padding()
                        
                        // Info message
                        Text("Profile information can only be edited by administrators")
                            .font(.caption)
                            .foregroundColor(.gray)
                            .multilineTextAlignment(.center)
                            .padding()
                        
                        // Logout button
                        if !sessionManager.isGuest() {
                            Button(action: {
                                sessionManager.logout()
                            }) {
                                Text("Logout")
                                    .fontWeight(.semibold)
                                    .foregroundColor(.red)
                                    .frame(maxWidth: .infinity)
                            }
                            .padding()
                        }
                    }
                    .padding(.top, 60)
                    .padding(.bottom, 100)
                }
            }
        }
        .kleosBackground()
        .navigationTitle("Profile")
        .navigationBarTitleDisplayMode(.large)
        .task {
            if profile == nil {
                loadProfile()
            }
        }
    }
    
    private func loadProfile() {
        isLoading = true
        errorMessage = nil
        
        if let currentUser = sessionManager.currentUser {
            self.profile = currentUser
        }
        
        // ЕСЛИ ГОСТЬ - НЕ ДЕЛАЕМ ЗАПРОС К /ME (избегаем 401)
        if sessionManager.isGuest() {
            print("👤 ProfileView: Guest mode, skipping API call")
            self.isLoading = false
            return
        }
        
        Task {
            do {
                let fetched = try await apiClient.getProfile()
                await MainActor.run {
                    self.profile = fetched
                    self.isLoading = false
                    self.errorMessage = nil
                }
            } catch {
                await MainActor.run {
                    self.isLoading = false
                    self.errorMessage = error.localizedDescription
                    if self.profile == nil {
                        self.errorMessage = "Failed to load profile. Using cached data."
                    }
                }
            }
        }
    }
}

struct ProfileField: View {
    let label: String
    let value: String
    var isMultiline: Bool = false
    
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(label)
                .font(.system(size: 14, weight: .semibold))
                .foregroundColor(.gray)
            
            Text(value.isEmpty ? "—" : value)
                .foregroundColor(.white)
                .padding()
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(Color.white.opacity(0.1))
                .cornerRadius(8)
                .frame(minHeight: isMultiline ? 100 : nil)
        }
    }
}
