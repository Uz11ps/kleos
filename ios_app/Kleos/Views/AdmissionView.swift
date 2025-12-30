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
    @State private var sex = "Male"
    @State private var passportNumber = ""
    @State private var passportIssue = ""
    @State private var passportExpiry = ""
    @State private var visaCity = ""
    @State private var program = ""
    @State private var comment = ""
    @State private var consentAccepted = false
    
    @State private var isLoading = false
    @State private var showConsent = false
    
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
                            
                            Picker("Sex", selection: $sex) {
                                Text("Male").tag("Male")
                                Text("Female").tag("Female")
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
        !phone.isEmpty &&
        !dateOfBirth.isEmpty &&
        !placeOfBirth.isEmpty &&
        !nationality.isEmpty &&
        !passportNumber.isEmpty &&
        !passportIssue.isEmpty &&
        !passportExpiry.isEmpty &&
        !visaCity.isEmpty &&
        !program.isEmpty &&
        consentAccepted
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
        isLoading = true
        
        let application = AdmissionApplication(
            firstName: firstName,
            lastName: lastName,
            patronymic: patronymic.isEmpty ? nil : patronymic,
            email: email,
            phone: phone,
            dateOfBirth: dateOfBirth,
            placeOfBirth: placeOfBirth,
            nationality: nationality,
            sex: sex,
            passportNumber: passportNumber,
            passportIssue: passportIssue,
            passportExpiry: passportExpiry,
            visaCity: visaCity,
            program: program,
            comment: comment.isEmpty ? nil : comment
        )
        
        Task {
            do {
                let response = try await apiClient.submitAdmission(application)
                await MainActor.run {
                    isLoading = false
                    // Show success message
                }
            } catch {
                await MainActor.run {
                    isLoading = false
                    // Show error message
                }
            }
        }
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

