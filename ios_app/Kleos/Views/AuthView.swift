import SwiftUI

struct AuthView: View {
    @StateObject private var sessionManager = SessionManager.shared
    @State private var showLogin = false
    @State private var showRegister = false
    
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
            
            VStack(spacing: 30) {
                Spacer()
                
                VStack(spacing: 16) {
                    Text("Welcome")
                        .font(.system(size: 40, weight: .bold))
                        .foregroundColor(.white)
                    
                    Text("Log in or create a profile")
                        .font(.system(size: 16))
                        .foregroundColor(.gray)
                }
                
                Spacer()
                
                VStack(spacing: 12) {
                    Button(action: {
                        showLogin = true
                    }) {
                        Text("Sign In")
                            .font(.system(size: 24, weight: .semibold))
                            .frame(width: 214, height: 62)
                    }
                    .buttonStyle(KleosButtonStyle())
                    
                    Button(action: {
                        showRegister = true
                    }) {
                        Text("Sign Up")
                            .font(.system(size: 24, weight: .semibold))
                            .frame(width: 214, height: 62)
                    }
                    .buttonStyle(KleosOutlinedButtonStyle())
                    
                    Button(action: {
                        // Guest login
                        sessionManager.saveUser(fullName: "Guest", email: "guest@local")
                        sessionManager.saveToken(UUID().uuidString)
                    }) {
                        Text("Or login as guest")
                            .font(.system(size: 14))
                            .foregroundColor(.gray)
                    }
                    .padding(.top, 12)
                }
                .padding(.bottom, 32)
            }
        }
        .sheet(isPresented: $showLogin) {
            LoginView()
        }
        .sheet(isPresented: $showRegister) {
            RegisterView()
        }
        .fullScreenCover(isPresented: $sessionManager.isLoggedIn) {
            MainTabView()
        }
    }
}

struct LoginView: View {
    @Environment(\.dismiss) var dismiss
    @StateObject private var apiClient = ApiClient.shared
    @StateObject private var sessionManager = SessionManager.shared
    
    @State private var email = ""
    @State private var password = ""
    @State private var isLoading = false
    @State private var errorMessage: String?
    
    var body: some View {
        NavigationView {
            ZStack {
                Color.white.ignoresSafeArea()
                
                VStack(spacing: 24) {
                    Text("Sign In")
                        .font(.system(size: 32, weight: .bold))
                        .foregroundColor(.black)
                        .padding(.top, 32)
                    
                    VStack(spacing: 16) {
                        TextField("Email", text: $email)
                            .textFieldStyle(.roundedBorder)
                            .keyboardType(.emailAddress)
                            .autocapitalization(.none)
                        
                        SecureField("Password", text: $password)
                            .textFieldStyle(.roundedBorder)
                        
                        if let error = errorMessage {
                            Text(error)
                                .foregroundColor(.red)
                                .font(.caption)
                        }
                        
                        Button(action: performLogin) {
                            if isLoading {
                                ProgressView()
                                    .tint(.white)
                            } else {
                                Text("Sign In")
                                    .fontWeight(.semibold)
                            }
                        }
                        .buttonStyle(KleosButtonStyle(backgroundColor: .blue, foregroundColor: .white))
                        .disabled(isLoading || email.isEmpty || password.isEmpty)
                    }
                    .padding(.horizontal, 24)
                    
                    Spacer()
                }
            }
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Close") {
                        dismiss()
                    }
                }
            }
        }
    }
    
    private func performLogin() {
        isLoading = true
        errorMessage = nil
        
        Task {
            do {
                let response = try await apiClient.login(email: email, password: password)
                
                if let token = response.token, let user = response.user {
                    await MainActor.run {
                        sessionManager.saveToken(token)
                        sessionManager.saveUser(fullName: user.fullName, email: user.email, role: user.role)
                        dismiss()
                    }
                } else if let error = response.error {
                    await MainActor.run {
                        errorMessage = error
                        isLoading = false
                    }
                }
            } catch {
                await MainActor.run {
                    errorMessage = "Login failed. Please try again."
                    isLoading = false
                }
            }
        }
    }
}

struct RegisterView: View {
    @Environment(\.dismiss) var dismiss
    @StateObject private var apiClient = ApiClient.shared
    @StateObject private var sessionManager = SessionManager.shared
    
    @State private var fullName = ""
    @State private var email = ""
    @State private var password = ""
    @State private var isLoading = false
    @State private var errorMessage: String?
    @State private var showVerifyEmail = false
    @State private var verifyEmail = ""
    
    var body: some View {
        NavigationView {
            ZStack {
                Color.white.ignoresSafeArea()
                
                VStack(spacing: 24) {
                    Text("Sign Up")
                        .font(.system(size: 32, weight: .bold))
                        .foregroundColor(.black)
                        .padding(.top, 32)
                    
                    VStack(spacing: 16) {
                        TextField("Full Name", text: $fullName)
                            .textFieldStyle(.roundedBorder)
                        
                        TextField("Email", text: $email)
                            .textFieldStyle(.roundedBorder)
                            .keyboardType(.emailAddress)
                            .autocapitalization(.none)
                        
                        SecureField("Password", text: $password)
                            .textFieldStyle(.roundedBorder)
                        
                        if let error = errorMessage {
                            Text(error)
                                .foregroundColor(.red)
                                .font(.caption)
                        }
                        
                        Button(action: performRegister) {
                            if isLoading {
                                ProgressView()
                                    .tint(.white)
                            } else {
                                Text("Sign Up")
                                    .fontWeight(.semibold)
                            }
                        }
                        .buttonStyle(KleosButtonStyle(backgroundColor: .blue, foregroundColor: .white))
                        .disabled(isLoading || fullName.isEmpty || email.isEmpty || password.isEmpty)
                    }
                    .padding(.horizontal, 24)
                    
                    Spacer()
                }
            }
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Close") {
                        dismiss()
                    }
                }
            }
            .sheet(isPresented: $showVerifyEmail) {
                VerifyEmailView(email: verifyEmail)
            }
        }
    }
    
    private func performRegister() {
        isLoading = true
        errorMessage = nil
        
        Task {
            do {
                let response = try await apiClient.register(fullName: fullName, email: email, password: password)
                
                if response.requiresVerification == true {
                    await MainActor.run {
                        verifyEmail = email
                        showVerifyEmail = true
                        dismiss()
                    }
                } else if let error = response.error {
                    await MainActor.run {
                        errorMessage = error
                        isLoading = false
                    }
                }
            } catch {
                await MainActor.run {
                    errorMessage = "Registration failed. Please try again."
                    isLoading = false
                }
            }
        }
    }
}

struct VerifyEmailView: View {
    let email: String
    @Environment(\.dismiss) var dismiss
    
    var body: some View {
        NavigationView {
            VStack(spacing: 24) {
                Text("Verify Email")
                    .font(.system(size: 32, weight: .bold))
                
                Text("We sent an email with a verification link. Open the link from the email on this device to complete registration.")
                    .multilineTextAlignment(.center)
                    .padding(.horizontal)
                
                Text("Address: \(email)")
                    .font(.caption)
                    .foregroundColor(.gray)
                
                Spacer()
            }
            .padding()
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Close") {
                        dismiss()
                    }
                }
            }
        }
    }
}

