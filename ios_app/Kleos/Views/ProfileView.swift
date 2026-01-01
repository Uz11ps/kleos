import SwiftUI

struct ProfileView: View {
    @StateObject private var apiClient = ApiClient.shared
    @StateObject private var sessionManager = SessionManager.shared
    @State private var profile: UserProfile?
    @State private var isLoading = true
    @State private var isEditing = false
    
    @State private var fullName = ""
    @State private var phone = ""
    @State private var course = ""
    @State private var speciality = ""
    @State private var status = ""
    @State private var university = ""
    @State private var payment = ""
    @State private var penalties = ""
    @State private var notes = ""
    
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
                } else if let profile = profile {
                    ScrollView {
                        VStack(spacing: 24) {
                            // Avatar
                            AsyncImage(url: apiClient.getFullUrl(profile.avatarUrl)) { phase in
                                if case .success(let image) = phase {
                                    image
                                        .resizable()
                                        .aspectRatio(contentMode: .fill)
                                } else {
                                    Image(systemName: "person.circle.fill")
                                        .resizable()
                                }
                            }
                            .frame(width: 100, height: 100)
                            .clipShape(Circle())
                            .foregroundColor(.gray)
                            
                            // Profile fields
                            VStack(spacing: 16) {
                                ProfileField(label: "Full Name", value: $fullName, isEditable: isEditing)
                                ProfileField(label: "Email", value: .constant(profile.email), isEditable: false)
                                ProfileField(label: "Role", value: .constant(profile.role), isEditable: false)
                                ProfileField(label: "Student ID", value: .constant(profile.studentId ?? ""), isEditable: false)
                                ProfileField(label: "Phone", value: $phone, isEditable: isEditing)
                                ProfileField(label: "Course", value: $course, isEditable: isEditing)
                                ProfileField(label: "Speciality", value: $speciality, isEditable: isEditing)
                                ProfileField(label: "Status", value: $status, isEditable: isEditing)
                                ProfileField(label: "University", value: $university, isEditable: isEditing)
                                ProfileField(label: "Payment", value: $payment, isEditable: isEditing)
                                ProfileField(label: "Penalties", value: $penalties, isEditable: isEditing)
                                ProfileField(label: "Notes", value: $notes, isEditable: isEditing, isMultiline: true)
                            }
                            .padding()
                            
                            if isEditing {
                                Button(action: saveProfile) {
                                    Text("Save")
                                        .fontWeight(.semibold)
                                        .frame(maxWidth: .infinity)
                                }
                                .buttonStyle(KleosButtonStyle())
                                .padding()
                            }
                            
                            // Logout button
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
                        .padding(.top, 20)
                    }
                }
            }
            .navigationTitle("Profile")
            .navigationBarTitleDisplayMode(.large)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(isEditing ? "Cancel" : "Edit") {
                        if isEditing {
                            loadProfile()
                        }
                        isEditing.toggle()
                    }
                }
            }
            .onAppear {
                loadProfile()
            }
        }
    }
    
    private func loadProfile() {
        isLoading = true
        
        Task {
            do {
                let fetched = try await apiClient.getProfile()
                await MainActor.run {
                    self.profile = fetched
                    self.fullName = fetched.fullName
                    self.phone = fetched.phone ?? ""
                    self.course = fetched.course ?? ""
                    self.speciality = fetched.speciality ?? ""
                    self.status = fetched.status ?? ""
                    self.university = fetched.university ?? ""
                    self.payment = fetched.payment ?? ""
                    self.penalties = fetched.penalties ?? ""
                    self.notes = fetched.notes ?? ""
                    self.isLoading = false
                }
            } catch {
                await MainActor.run {
                    self.isLoading = false
                }
            }
        }
    }
    
    private func saveProfile() {
        let request = UpdateProfileRequest(
            fullName: fullName,
            phone: phone.isEmpty ? nil : phone,
            course: course.isEmpty ? nil : course,
            speciality: speciality.isEmpty ? nil : speciality,
            status: status.isEmpty ? nil : status,
            university: university.isEmpty ? nil : university,
            payment: payment.isEmpty ? nil : payment,
            penalties: penalties.isEmpty ? nil : penalties,
            notes: notes.isEmpty ? nil : notes
        )
        
        Task {
            do {
                let updated = try await apiClient.updateProfile(request)
                await MainActor.run {
                    self.profile = updated
                    self.isEditing = false
                }
            } catch {
                // Handle error
            }
        }
    }
}

struct ProfileField: View {
    let label: String
    @Binding var value: String
    let isEditable: Bool
    var isMultiline: Bool = false
    
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(label)
                .font(.system(size: 14, weight: .semibold))
                .foregroundColor(.gray)
            
            if isEditable {
                if isMultiline {
                    TextEditor(text: $value)
                        .frame(height: 100)
                        .background(Color.white.opacity(0.1))
                        .foregroundColor(.white)
                        .cornerRadius(8)
                } else {
                    TextField("", text: $value)
                        .textFieldStyle(.plain)
                        .foregroundColor(.white)
                        .padding()
                        .background(Color.white.opacity(0.1))
                        .cornerRadius(8)
                }
            } else {
                Text(value.isEmpty ? "—" : value)
                    .foregroundColor(.white)
                    .padding()
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(Color.white.opacity(0.1))
                    .cornerRadius(8)
            }
        }
    }
}

