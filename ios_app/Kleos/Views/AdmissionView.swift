import SwiftUI

struct AdmissionView: View {
    @StateObject private var apiClient = ApiClient.shared
    @State private var countries: [Country] = []
    @State private var consentText = ""
    
    // Form fields
    @State private var firstName = ""
    @State private var lastName = ""
    @State private var patronymic = ""
    @State private var email = ""
    @State private var phone = ""
    @State private var dateOfBirth = ""
    @State private var placeOfBirth = ""
    @State private var nationality = ""
    @State private var passportNumber = ""
    @State private var passportIssue = ""
    @State private var passportExpiry = ""
    @State private var visaCity = ""
    @State private var program = ""
    @State private var comment = ""
    @State private var consentAccepted = false
    
    @State private var isLoading = false
    @State private var showConsent = false
    @State private var errorMessage: String?
    @State private var successMessage: String?
    
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
                
                ScrollView {
                    VStack(spacing: 24) {
                        Text("Admission Application")
                            .font(.system(size: 32, weight: .bold))
                            .foregroundColor(.white)
                            .padding()
                        
                        VStack(spacing: 16) {
                            TextField("First Name", text: $firstName)
                                .textFieldStyle(KleosTextFieldStyle())
                            
                            TextField("Last Name", text: $lastName)
                                .textFieldStyle(KleosTextFieldStyle())
                            
                            TextField("Patronymic (optional)", text: $patronymic)
                                .textFieldStyle(KleosTextFieldStyle())
                            
                            TextField("Email", text: $email)
                                .textFieldStyle(KleosTextFieldStyle())
                                .keyboardType(.emailAddress)
                                .autocapitalization(.none)
                            
                            TextField("Phone", text: $phone)
                                .textFieldStyle(KleosTextFieldStyle())
                                .keyboardType(.phonePad)
                            
                            TextField("Date of Birth", text: $dateOfBirth)
                                .textFieldStyle(KleosTextFieldStyle())
                            
                            TextField("Place of Birth", text: $placeOfBirth)
                                .textFieldStyle(KleosTextFieldStyle())
                            
                            Picker("Nationality", selection: $nationality) {
                                Text("Select").tag("")
                                ForEach(countries) { country in
                                    Text(country.name).tag(country.name)
                                }
                            }
                            .pickerStyle(.menu)
                            .foregroundColor(.white)
                            
                            TextField("Passport Number", text: $passportNumber)
                                .textFieldStyle(KleosTextFieldStyle())
                            
                            TextField("Date and Place of Passport Issue", text: $passportIssue)
                                .textFieldStyle(KleosTextFieldStyle())
                            
                            TextField("Passport Expiry Date", text: $passportExpiry)
                                .textFieldStyle(KleosTextFieldStyle())
                            
                            TextField("City of Russian Visa Obtaining", text: $visaCity)
                                .textFieldStyle(KleosTextFieldStyle())
                            
                            TextField("Program", text: $program)
                                .textFieldStyle(KleosTextFieldStyle())
                            
                            TextField("Comment (optional)", text: $comment, axis: .vertical)
                                .textFieldStyle(KleosTextFieldStyle())
                                .lineLimit(3...6)
                            
                            // Consent checkbox
                            HStack {
                                Button(action: {
                                    consentAccepted.toggle()
                                }) {
                                    Image(systemName: consentAccepted ? "checkmark.square.fill" : "square")
                                        .foregroundColor(.white)
                                }
                                
                                HStack(spacing: 4) {
                                    Text("I agree to the processing of personal data")
                                    Button(action: {
                                        showConsent = true
                                    }) {
                                        Text("Read full text")
                                            .underline()
                                    }
                                }
                                .font(.system(size: 14))
                                .foregroundColor(.white)
                            }
                            
                            if let error = errorMessage {
                                Text(error)
                                    .foregroundColor(.red)
                                    .font(.caption)
                            }
                            
                            if let success = successMessage {
                                Text(success)
                                    .foregroundColor(.green)
                                    .font(.caption)
                            }
                            
                            Button(action: submitApplication) {
                                if isLoading {
                                    ProgressView()
                                        .tint(.white)
                                } else {
                                    Text("Submit Application")
                                        .fontWeight(.semibold)
                                }
                            }
                            .buttonStyle(KleosButtonStyle())
                            .disabled(isLoading || !isFormValid)
                        }
                        .padding()
                    }
                }
            }
            .navigationTitle("Admission")
            .navigationBarTitleDisplayMode(.large)
            .sheet(isPresented: $showConsent) {
                ConsentTextView(text: consentText)
            }
            .onAppear {
                loadData()
            }
        }
    }
    
    private var isFormValid: Bool {
        !firstName.isEmpty &&
        !lastName.isEmpty &&
        !email.isEmpty &&
        isValidEmail(email) &&
        !phone.isEmpty &&
        !program.isEmpty &&
        consentAccepted
    }
    
    private func isValidEmail(_ email: String) -> Bool {
        let emailRegex = "[A-Z0-9a-z._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,64}"
        let emailPredicate = NSPredicate(format:"SELF MATCHES %@", emailRegex)
        return emailPredicate.evaluate(with: email)
    }
    
    private func loadData() {
        loadCountries()
        loadConsentText()
    }
    
    private func loadCountries() {
        Task {
            do {
                let fetched = try await apiClient.fetchCountries()
                await MainActor.run {
                    self.countries = fetched
                }
            } catch {
                // Handle error
            }
        }
    }
    
    private func loadConsentText() {
        Task {
            do {
                let fetched = try await apiClient.fetchConsentText(language: "en")
                await MainActor.run {
                    self.consentText = fetched.text
                }
            } catch {
                // Handle error
            }
        }
    }
    
    private func submitApplication() {
        // Валидация
        if firstName.isEmpty {
            errorMessage = "Введите имя"
            return
        }
        if lastName.isEmpty {
            errorMessage = "Введите фамилию"
            return
        }
        if email.isEmpty {
            errorMessage = "Введите email"
            return
        }
        if !isValidEmail(email) {
            errorMessage = "Некорректный email"
            return
        }
        if phone.isEmpty {
            errorMessage = "Введите телефон"
            return
        }
        if program.isEmpty {
            errorMessage = "Выберите программу"
            return
        }
        if !consentAccepted {
            errorMessage = "Необходимо согласие на обработку персональных данных"
            return
        }
        
        isLoading = true
        errorMessage = nil
        successMessage = nil
        
        let application = AdmissionApplication(
            firstName: firstName,
            lastName: lastName,
            patronymic: patronymic.isEmpty ? nil : patronymic,
            email: email,
            phone: phone,
            dateOfBirth: dateOfBirth.isEmpty ? nil : dateOfBirth,
            placeOfBirth: placeOfBirth.isEmpty ? nil : placeOfBirth,
            nationality: nationality.isEmpty ? nil : nationality,
            passportNumber: passportNumber.isEmpty ? nil : passportNumber,
            passportIssue: passportIssue.isEmpty ? nil : passportIssue,
            passportExpiry: passportExpiry.isEmpty ? nil : passportExpiry,
            visaCity: visaCity.isEmpty ? nil : visaCity,
            program: program,
            comment: comment.isEmpty ? nil : comment
        )
        
        Task {
            do {
                let response = try await apiClient.submitAdmission(application)
                await MainActor.run {
                    isLoading = false
                    if response.ok == true {
                        successMessage = "Заявка успешно отправлена!"
                        // Очистить форму через несколько секунд
                        DispatchQueue.main.asyncAfter(deadline: .now() + 3) {
                            resetForm()
                        }
                    } else if let error = response.error {
                        errorMessage = error
                    }
                }
            } catch let error as ApiError {
                await MainActor.run {
                    isLoading = false
                    switch error {
                    case .httpError(let code):
                        errorMessage = "Ошибка отправки заявки (Error \(code)). Попробуйте снова."
                    case .serverError(let message):
                        errorMessage = message
                    default:
                        errorMessage = "Ошибка отправки заявки. Проверьте подключение к интернету."
                    }
                }
            } catch {
                await MainActor.run {
                    isLoading = false
                    errorMessage = "Ошибка отправки заявки. Попробуйте снова."
                }
            }
        }
    }
    
    private func resetForm() {
        firstName = ""
        lastName = ""
        patronymic = ""
        email = ""
        phone = ""
        dateOfBirth = ""
        placeOfBirth = ""
        nationality = ""
        passportNumber = ""
        passportIssue = ""
        passportExpiry = ""
        visaCity = ""
        program = ""
        comment = ""
        consentAccepted = false
        successMessage = nil
        errorMessage = nil
    }
}

struct KleosTextFieldStyle: TextFieldStyle {
    func _body(configuration: TextField<Self._Label>) -> some View {
        configuration
            .padding()
            .background(Color.white.opacity(0.1))
            .foregroundColor(.white)
            .cornerRadius(8)
    }
}

struct ConsentTextView: View {
    let text: String
    @Environment(\.dismiss) var dismiss
    
    var body: some View {
        NavigationView {
            ScrollView {
                Text(text)
                    .padding()
            }
            .navigationTitle("Consent Text")
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

