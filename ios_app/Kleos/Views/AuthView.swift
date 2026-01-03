import SwiftUI
import Foundation

struct AuthView: View {
    @ObservedObject private var sessionManager = SessionManager.shared
    @StateObject private var localizationManager = LocalizationManager.shared
    @State private var showLogin = false
    @State private var showRegister = false
    @State private var showVerifyEmail = false
    @State private var verifyEmail = ""
    
    var body: some View {
        GeometryReader { geo in
            ZStack {
                VStack(spacing: 0) {
                    // Большой отступ сверху, чтобы текст был ниже ленты
                    Spacer()
                        .frame(height: geo.size.height * 0.3)
                    
                    VStack(spacing: 12) {
                        Text(t("welcome"))
                            .font(.system(size: 38, weight: .bold))
                            .foregroundColor(.white)
                            .multilineTextAlignment(.center)
                        
                        Text(t("auth_description"))
                            .font(.system(size: 16))
                            .foregroundColor(.white.opacity(0.7))
                            .multilineTextAlignment(.center)
                    }
                    .padding(.horizontal, 30)
                    
                    Spacer()
                    
                    VStack(spacing: 16) {
                        Button(action: { 
                            sessionManager.logout()
                            showLogin = true 
                        }) {
                            Text(t("sign_in"))
                                .font(.system(size: 20, weight: .bold))
                                .frame(maxWidth: .infinity)
                                .frame(height: 60)
                        }
                        .buttonStyle(KleosButtonStyle(backgroundColor: .white, foregroundColor: .black))
                        
                        Button(action: { 
                            sessionManager.logout()
                            showRegister = true 
                        }) {
                            Text(t("sign_up"))
                                .font(.system(size: 20, weight: .bold))
                                .frame(maxWidth: .infinity)
                                .frame(height: 60)
                        }
                        .buttonStyle(KleosOutlinedButtonStyle(strokeColor: .white, foregroundColor: .white))
                        
                        Button(action: {
                            sessionManager.saveUser(fullName: t("guest"), email: "guest@local")
                            sessionManager.saveToken(UUID().uuidString)
                        }) {
                            Text(t("login_as_guest"))
                                .font(.system(size: 14))
                                .foregroundColor(.white.opacity(0.6))
                        }
                        .padding(.top, 10)
                    }
                    .padding(.horizontal, 40)
                    .padding(.bottom, 60)
                }
            }
        }
        .kleosBackground(showGradientShape: true, circlePositions: .center, isSplashOrAuth: true) 
        .sheet(isPresented: $showLogin) { LoginView() }
        .sheet(isPresented: $showRegister) { 
            RegisterView(onSuccess: { email in
                self.verifyEmail = email
                self.showVerifyEmail = true
            }) 
        }
        .sheet(isPresented: $showVerifyEmail) { VerifyEmailView(email: verifyEmail) }
    }
}

struct KleosInputField: View {
    let label: String
    let icon: String
    let placeholder: String
    @Binding var text: String
    var isSecure: Bool = false
    var keyboardType: UIKeyboardType = .default
    var autoCap: UITextAutocapitalizationType = .none
    
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(label)
                .font(.system(size: 14))
                .foregroundColor(.gray)
            
            HStack(spacing: 12) {
                Image(systemName: icon)
                    .foregroundColor(.gray)
                    .frame(width: 24)
                
                if isSecure {
                    SecureField(placeholder, text: $text)
                        .foregroundColor(.black)
                } else {
                    TextField(placeholder, text: $text)
                        .foregroundColor(.black)
                        .keyboardType(keyboardType)
                        .autocapitalization(autoCap)
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 18)
            .background(Color(hex: "F3F4F6"))
            .cornerRadius(12)
        }
    }
}

struct LoginView: View {
    @Environment(\.dismiss) var dismiss
    @StateObject private var apiClient = ApiClient.shared
    @ObservedObject private var sessionManager = SessionManager.shared
    @StateObject private var localizationManager = LocalizationManager.shared
    @State private var email = ""
    @State private var password = ""
    @State private var rememberPassword = false
    @State private var isLoading = false
    @State private var errorMessage: String?
    
    var body: some View {
        ZStack(alignment: .bottom) {
            Color.clear.kleosBackground(showGradientShape: true, circlePositions: .center, isSplashOrAuth: true)
                .onTapGesture { dismiss() }
            
            VStack(spacing: 0) {
                Capsule()
                    .fill(Color.gray.opacity(0.3))
                    .frame(width: 40, height: 6)
                    .padding(.top, 12)
                
                VStack(spacing: 24) {
                    Text(t("sign_in"))
                        .font(.system(size: 32, weight: .bold))
                        .foregroundColor(.black)
                        .frame(maxWidth: .infinity, alignment: .leading)
                    
                    VStack(spacing: 20) {
                        KleosInputField(label: t("email"), icon: "envelope", placeholder: t("email"), text: $email, keyboardType: .emailAddress)
                        
                        KleosInputField(label: t("password"), icon: "lock", placeholder: t("password"), text: $password, isSecure: true)
                        
                        HStack {
                            Button(action: { rememberPassword.toggle() }) {
                                HStack(spacing: 8) {
                                    Image(systemName: rememberPassword ? "checkmark.circle.fill" : "circle")
                                        .foregroundColor(rememberPassword ? .black : .gray)
                                    Text(t("remember_password"))
                                        .font(.system(size: 14))
                                        .foregroundColor(.gray)
                                }
                            }
                            
                            Spacer()
                            
                            Button(action: {}) {
                                Text(t("forgot_password"))
                                    .font(.system(size: 14))
                                    .foregroundColor(.gray)
                            }
                        }
                        
                        if let error = errorMessage {
                            Text(error).foregroundColor(.red).font(.system(size: 14, weight: .medium)).multilineTextAlignment(.center)
                        }
                        
                        Button(action: performLogin) {
                            if isLoading { ProgressView().tint(.white) }
                            else { 
                                Text(t("sign_in"))
                                    .font(.system(size: 24, weight: .bold))
                            }
                        }
                        .buttonStyle(KleosButtonStyle(backgroundColor: Color(hex: "2D262D"), foregroundColor: .white))
                        .padding(.top, 8)
                        .disabled(isLoading || email.isEmpty || password.isEmpty)
                        
                        Button(action: { dismiss() }) {
                            HStack(spacing: 4) {
                                Text(t("no_account_yet"))
                                    .foregroundColor(.gray)
                                Text(t("sign_up"))
                                    .foregroundColor(.black)
                                    .fontWeight(.bold)
                            }
                            .font(.system(size: 14))
                        }
                        .padding(.top, 10)
                    }
                }
                .padding(24)
                .padding(.bottom, 40)
            }
            .background(Color.white)
            .cornerRadius(24, corners: [.topLeft, .topRight])
            .padding(.bottom, 100)
        }
        .ignoresSafeArea(.keyboard)
    }
    
    private func performLogin() {
        if email.isEmpty { errorMessage = t("enter_email"); return }
        if password.isEmpty { errorMessage = t("enter_password"); return }
        isLoading = true; errorMessage = nil
        Task {
            do {
                let response = try await apiClient.login(email: email, password: password)
                if let token = response.token, let user = response.user {
                    await MainActor.run {
                        sessionManager.saveToken(token)
                        sessionManager.saveUser(fullName: user.fullName, email: user.email, role: user.role)
                        dismiss()
                    }
                } else if let error = response.error { await MainActor.run { errorMessage = error; isLoading = false } }
            } catch { await MainActor.run { errorMessage = t("login_error"); isLoading = false } }
        }
    }
}

struct RegisterView: View {
    @Environment(\.dismiss) var dismiss
    let onSuccess: (String) -> Void
    @StateObject private var apiClient = ApiClient.shared
    @ObservedObject private var sessionManager = SessionManager.shared
    @StateObject private var localizationManager = LocalizationManager.shared
    @State private var fullName = ""
    @State private var email = ""
    @State private var password = ""
    @State private var agreeToTerms = false
    @State private var isLoading = false
    @State private var errorMessage: String?
    
    var body: some View {
        ZStack(alignment: .bottom) {
            Color.clear.kleosBackground(showGradientShape: true, circlePositions: .center, isSplashOrAuth: true)
                .onTapGesture { dismiss() }
            
            VStack(spacing: 0) {
                Capsule()
                    .fill(Color.gray.opacity(0.3))
                    .frame(width: 40, height: 6)
                    .padding(.top, 12)
                
                VStack(spacing: 24) {
                    Text(t("sign_up"))
                        .font(.system(size: 32, weight: .bold))
                        .foregroundColor(.black)
                        .frame(maxWidth: .infinity, alignment: .leading)
                    
                    VStack(spacing: 20) {
                        KleosInputField(label: t("full_name"), icon: "person", placeholder: t("full_name"), text: $fullName, autoCap: .words)
                        
                        KleosInputField(label: t("email"), icon: "envelope", placeholder: t("email"), text: $email, keyboardType: .emailAddress)
                        
                        KleosInputField(label: t("password"), icon: "lock", placeholder: t("password"), text: $password, isSecure: true)
                        
                        Button(action: { agreeToTerms.toggle() }) {
                            HStack(spacing: 8) {
                                Image(systemName: agreeToTerms ? "checkmark.circle.fill" : "circle")
                                    .foregroundColor(agreeToTerms ? .black : .gray)
                                Text(t("agree_processing"))
                                    .font(.system(size: 14))
                                    .foregroundColor(.gray)
                                    .multilineTextAlignment(.leading)
                            }
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                        
                        if let error = errorMessage {
                            Text(error).foregroundColor(.red).font(.system(size: 14, weight: .medium)).multilineTextAlignment(.center)
                        }
                        
                        Button(action: performRegister) {
                            if isLoading { ProgressView().tint(.white) }
                            else { 
                                Text(t("sign_up"))
                                    .font(.system(size: 24, weight: .bold))
                            }
                        }
                        .buttonStyle(KleosButtonStyle(backgroundColor: Color(hex: "2D262D"), foregroundColor: .white))
                        .padding(.top, 8)
                        .disabled(isLoading || fullName.isEmpty || email.isEmpty || password.isEmpty || !agreeToTerms)
                        
                        Button(action: { dismiss() }) {
                            HStack(spacing: 4) {
                                Text(t("already_have_account"))
                                    .foregroundColor(.gray)
                                Text(t("sign_in"))
                                    .foregroundColor(.black)
                                    .fontWeight(.bold)
                            }
                            .font(.system(size: 14))
                        }
                        .padding(.top, 10)
                    }
                }
                .padding(24)
                .padding(.bottom, 40)
            }
            .background(Color.white)
            .cornerRadius(24, corners: [.topLeft, .topRight])
            .padding(.bottom, 60)
        }
        .ignoresSafeArea(.keyboard)
    }
    
    private func performRegister() {
        if fullName.isEmpty { errorMessage = t("enter_full_name"); return }
        if email.isEmpty { errorMessage = t("enter_email"); return }
        if password.count < 6 { errorMessage = t("password_too_short"); return }
        
        isLoading = true; errorMessage = nil
        Task {
            do {
                let response = try await apiClient.register(fullName: fullName, email: email, password: password)
                await MainActor.run {
                    if response.requiresVerification == true {
                        onSuccess(email)
                        dismiss()
                    } else if let token = response.token, let user = response.user {
                        sessionManager.saveToken(token)
                        sessionManager.saveUser(fullName: user.fullName, email: user.email, role: user.role)
                        dismiss()
                    } else {
                        onSuccess(email)
                        dismiss()
                    }
                }
            } catch let error as ApiError {
                await MainActor.run {
                    switch error {
                    case .serverError(let msg): errorMessage = msg
                    default: errorMessage = t("register_error")
                    }
                    isLoading = false
                }
            } catch {
                await MainActor.run {
                    errorMessage = error.localizedDescription
                    isLoading = false
                }
            }
        }
    }
}

struct VerifyEmailView: View {
    let email: String
    @Environment(\.dismiss) var dismiss
    @ObservedObject private var sessionManager = SessionManager.shared
    @StateObject private var apiClient = ApiClient.shared
    @State private var checkTimer: Timer?
    @State private var isLoading = false
    @State private var errorMessage: String?
    
    var body: some View {
        ZStack {
            Color.white.ignoresSafeArea()
            VStack(spacing: 0) {
                Spacer()
                VStack(spacing: 24) {
                    Image(systemName: "envelope.badge.shield.half.filled")
                        .font(.system(size: 80))
                        .foregroundColor(Color.kleosBlue)
                    
                    Text(LocalizationManager.shared.t("verify_email"))
                        .font(.system(size: 32, weight: .bold))
                        .foregroundColor(.black)
                    
                    Text(LocalizationManager.shared.t("verify_email_description"))
                        .multilineTextAlignment(.center)
                        .padding(.horizontal)
                        .foregroundColor(.black.opacity(0.7))
                    
                    Text("\(LocalizationManager.shared.t("address")): \(email)")
                        .font(.system(size: 16, weight: .medium))
                        .foregroundColor(Color.kleosBlue)
                    
                    if let error = errorMessage {
                        Text(error)
                            .foregroundColor(.red)
                            .font(.system(size: 14))
                            .multilineTextAlignment(.center)
                            .padding(.horizontal)
                    }
                    
                    Button(action: checkVerificationStatus) {
                        if isLoading {
                            ProgressView().tint(.white)
                        } else {
                            Text(LocalizationManager.shared.t("check_status"))
                                .fontWeight(.semibold)
                        }
                    }
                    .buttonStyle(KleosButtonStyle(backgroundColor: Color.kleosBlue, foregroundColor: .white))
                    .padding(.top, 20)
                    .disabled(isLoading)
                }
                .padding()
                Spacer()
                Spacer()
            }
        }
        .onAppear {
            checkTimer = Timer.scheduledTimer(withTimeInterval: 2.0, repeats: true) { _ in
                if let token = sessionManager.getToken(), token.contains("."), !sessionManager.isGuest() {
                    DispatchQueue.main.async {
                        dismiss()
                    }
                }
            }
        }
        .onDisappear {
            checkTimer?.invalidate()
        }
    }
    
    private func checkVerificationStatus() {
        isLoading = true
        errorMessage = nil
        Task {
            do {
                if let token = sessionManager.getToken(), token.contains(".") {
                    let profile = try await ApiClient.shared.getProfile()
                    await MainActor.run {
                        sessionManager.saveUser(fullName: profile.fullName, email: profile.email, role: profile.role)
                        isLoading = false
                        dismiss()
                    }
                } else {
                    await MainActor.run {
                        isLoading = false
                        errorMessage = LocalizationManager.shared.t("verification_pending")
                    }
                }
            } catch {
                await MainActor.run {
                    isLoading = false
                    errorMessage = LocalizationManager.shared.t("check_failed")
                }
            }
        }
    }
}

extension View {
    func cornerRadius(_ radius: CGFloat, corners: UIRectCorner) -> some View {
        clipShape(RoundedCorner(radius: radius, corners: corners))
    }
}

struct RoundedCorner: Shape {
    var radius: CGFloat = .infinity
    var corners: UIRectCorner = .allCorners
    func path(in rect: CGRect) -> Path {
        let path = UIBezierPath(roundedRect: rect, byRoundingCorners: corners, cornerRadii: CGSize(width: radius, height: radius))
        return Path(path.cgPath)
    }
}
