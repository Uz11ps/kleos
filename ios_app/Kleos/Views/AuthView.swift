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
        ZStack {
            VStack(spacing: 0) {
                Spacer()
                
                VStack(spacing: 16) {
                    Text(t("welcome"))
                        .font(.system(size: 40, weight: .bold))
                        .foregroundColor(.white)
                        .multilineTextAlignment(.center)
                    Text(t("auth_description"))
                        .font(.system(size: 16))
                        .foregroundColor(.gray)
                        .multilineTextAlignment(.center)
                }
                .padding(.horizontal, 24)
                
                Spacer()
                
                VStack(spacing: 12) {
                    Button(action: { showLogin = true }) {
                        Text(t("sign_in")).font(.system(size: 24, weight: .semibold)).frame(width: 214, height: 62)
                    }.buttonStyle(KleosButtonStyle(backgroundColor: .white, foregroundColor: Color(hex: "0E080F")))
                    
                    Button(action: { showRegister = true }) {
                        Text(t("sign_up")).font(.system(size: 24, weight: .semibold)).frame(width: 214, height: 62)
                    }.buttonStyle(KleosOutlinedButtonStyle(strokeColor: .white, foregroundColor: .white))
                    
                    Button(action: {
                        sessionManager.saveUser(fullName: t("guest"), email: "guest@local")
                        sessionManager.saveToken(UUID().uuidString)
                    }) {
                        Text(t("login_as_guest")).font(.system(size: 14)).foregroundColor(Color(hex: "CBD5E1"))
                    }.padding(.top, 12)
                }
                .padding(.bottom, 50)
                
                Spacer()
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .padding(.bottom, 200)
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

struct LoginView: View {
    @Environment(\.dismiss) var dismiss
    @StateObject private var apiClient = ApiClient.shared
    @ObservedObject private var sessionManager = SessionManager.shared
    @StateObject private var localizationManager = LocalizationManager.shared
    @State private var email = ""
    @State private var password = ""
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
                
                VStack(spacing: 32) {
                    Text(t("sign_in"))
                        .font(.system(size: 32, weight: .bold))
                        .foregroundColor(.black)
                    
                    VStack(spacing: 16) {
                        VStack(alignment: .leading, spacing: 8) {
                            Text(t("email")).font(.system(size: 14)).foregroundColor(.gray)
                            TextField("", text: $email)
                                .textFieldStyle(PlainTextFieldStyle())
                                .foregroundColor(.black)
                                .keyboardType(.emailAddress)
                                .autocapitalization(.none)
                            Divider().background(Color.gray.opacity(0.5))
                        }
                        
                        VStack(alignment: .leading, spacing: 8) {
                            Text(t("password")).font(.system(size: 14)).foregroundColor(.gray)
                            SecureField("", text: $password)
                                .textFieldStyle(PlainTextFieldStyle())
                                .foregroundColor(.black)
                            Divider().background(Color.gray.opacity(0.5))
                        }
                        
                        if let error = errorMessage {
                            Text(error).foregroundColor(.red).font(.system(size: 14, weight: .medium)).multilineTextAlignment(.center)
                        }
                        
                        Button(action: performLogin) {
                            if isLoading { ProgressView().tint(.white) }
                            else { Text(t("sign_in")).fontWeight(.semibold) }
                        }
                        .buttonStyle(KleosButtonStyle(backgroundColor: Color.kleosBlue, foregroundColor: .white))
                        .padding(.top, 8)
                        .disabled(isLoading || email.isEmpty || password.isEmpty)
                    }
                }
                .padding(24)
                .padding(.bottom, 40)
            }
            .background(Color.white)
            .cornerRadius(24, corners: [.topLeft, .topRight])
            .padding(.bottom, 160) // Поднимаем блок вверх, оставляя место снизу
        }
        .ignoresSafeArea(.keyboard) // Предотвращаем автоматическое сжатие контента клавиатурой
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
                    
                    VStack(spacing: 16) {
                        VStack(alignment: .leading, spacing: 8) {
                            Text(t("full_name")).font(.system(size: 14)).foregroundColor(.gray)
                            TextField("", text: $fullName)
                                .textFieldStyle(PlainTextFieldStyle())
                                .foregroundColor(.black)
                            Divider().background(Color.gray.opacity(0.5))
                        }
                        
                        VStack(alignment: .leading, spacing: 8) {
                            Text(t("email")).font(.system(size: 14)).foregroundColor(.gray)
                            TextField("", text: $email)
                                .textFieldStyle(PlainTextFieldStyle())
                                .foregroundColor(.black)
                                .keyboardType(.emailAddress)
                                .autocapitalization(.none)
                            Divider().background(Color.gray.opacity(0.5))
                        }
                        
                        VStack(alignment: .leading, spacing: 8) {
                            Text(t("password")).font(.system(size: 14)).foregroundColor(.gray)
                            SecureField("", text: $password)
                                .textFieldStyle(PlainTextFieldStyle())
                                .foregroundColor(.black)
                            Divider().background(Color.gray.opacity(0.5))
                        }
                        
                        if let error = errorMessage {
                            Text(error).foregroundColor(.red).font(.system(size: 14, weight: .medium)).multilineTextAlignment(.center)
                        }
                        
                        Button(action: performRegister) {
                            if isLoading { ProgressView().tint(.white) }
                            else { Text(t("sign_up")).fontWeight(.semibold) }
                        }
                        .buttonStyle(KleosButtonStyle(backgroundColor: Color.kleosBlue, foregroundColor: .white))
                        .padding(.top, 8)
                        .disabled(isLoading || fullName.isEmpty || email.isEmpty || password.isEmpty)
                    }
                }
                .padding(24)
                .padding(.bottom, 40)
            }
            .background(Color.white)
            .cornerRadius(24, corners: [.topLeft, .topRight])
            .padding(.bottom, 160) // Поднимаем блок вверх
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
                    } else if let error = response.error {
                        errorMessage = error
                        isLoading = false
                    } else {
                        onSuccess(email)
                        dismiss()
                    }
                }
            } catch let error as ApiError {
                await MainActor.run {
                    switch error {
                    case .serverError(let msg): errorMessage = msg
                    case .httpError(let code): errorMessage = "Error \(code)"
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
    var body: some View {
        ZStack {
            Color.white.ignoresSafeArea()
            VStack(spacing: 0) {
                Spacer()
                VStack(spacing: 24) {
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
                }
                .padding()
                Spacer()
                Spacer()
            }
        }
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button(LocalizationManager.shared.t("close")) { dismiss() }
                    .foregroundColor(.black)
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
