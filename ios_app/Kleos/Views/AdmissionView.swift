import SwiftUI

struct AdmissionView: View {
    @StateObject private var apiClient = ApiClient.shared
    @StateObject private var localizationManager = LocalizationManager.shared
    @State private var countries: [Country] = []
    @State private var consentText = ""
    
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
    
    var body: some View {
        ZStack {
            ScrollView {
                VStack(spacing: 24) {
                    Color.clear.frame(height: 100) 
                    VStack(spacing: 16) {
                        TextField(t("first_name"), text: $firstName).textFieldStyle(KleosTextFieldStyle())
                        TextField(t("last_name"), text: $lastName).textFieldStyle(KleosTextFieldStyle())
                        TextField(t("patronymic"), text: $patronymic).textFieldStyle(KleosTextFieldStyle())
                        TextField(t("phone"), text: $phone).textFieldStyle(KleosTextFieldStyle()).keyboardType(.phonePad)
                        TextField(t("email"), text: $email).textFieldStyle(KleosTextFieldStyle()).keyboardType(.emailAddress).autocapitalization(.none)
                        TextField(t("date_of_birth"), text: $dateOfBirth).textFieldStyle(KleosTextFieldStyle())
                        TextField(t("place_of_birth"), text: $placeOfBirth).textFieldStyle(KleosTextFieldStyle())
                        Picker(t("nationality"), selection: $nationality) {
                            Text(t("select")).tag("")
                            ForEach(countries) { country in Text(country.name).tag(country.name) }
                        }
                        .pickerStyle(.menu).foregroundColor(.white).frame(maxWidth: .infinity, alignment: .leading).padding().background(Color.white.opacity(0.1)).cornerRadius(8)
                        TextField(t("passport_number"), text: $passportNumber).textFieldStyle(KleosTextFieldStyle())
                        TextField(t("passport_issue"), text: $passportIssue).textFieldStyle(KleosTextFieldStyle())
                        TextField(t("passport_expiry"), text: $passportExpiry).textFieldStyle(KleosTextFieldStyle())
                        TextField(t("visa_city"), text: $visaCity).textFieldStyle(KleosTextFieldStyle())
                        TextField(t("program"), text: $program).textFieldStyle(KleosTextFieldStyle())
                        TextField(t("comment"), text: $comment, axis: .vertical).textFieldStyle(KleosTextFieldStyle()).lineLimit(3...6)
                        HStack {
                            Button(action: { consentAccepted.toggle() }) { Image(systemName: consentAccepted ? "checkmark.square.fill" : "square").foregroundColor(.white) }
                            HStack(spacing: 4) {
                                Text(t("agree_processing"))
                                Button(action: { showConsent = true }) { Text(t("read_full_text")).underline() }
                            }.font(.system(size: 14)).foregroundColor(.white)
                        }.padding(.vertical, 8)
                        Button(action: submitApplication) {
                            if isLoading { ProgressView().tint(.white) }
                            else { Text(t("submit_application")).fontWeight(.semibold).frame(maxWidth: .infinity) }
                        }.buttonStyle(KleosButtonStyle()).disabled(isLoading || !isFormValid)
                    }.padding()
                    Color.clear.frame(height: 50)
                }
            }
        }
        .kleosBackground()
        .navigationTitle(t("admission"))
        .navigationBarTitleDisplayMode(.large)
        .sheet(isPresented: $showConsent) { ConsentTextView(text: consentText) }
        .task { loadData() }
        .onChange(of: localizationManager.currentLanguage) { _, _ in loadData() }
    }
    
    private var isFormValid: Bool {
        !firstName.isEmpty && !lastName.isEmpty && !email.isEmpty && !phone.isEmpty && !dateOfBirth.isEmpty && !placeOfBirth.isEmpty && !nationality.isEmpty && !passportNumber.isEmpty && !passportIssue.isEmpty && !passportExpiry.isEmpty && !visaCity.isEmpty && !program.isEmpty && consentAccepted
    }
    
    private func loadData() {
        loadCountries()
        loadConsentText()
    }
    
    private func loadCountries() {
        Task {
            do {
                let fetched = try await apiClient.fetchCountries()
                await MainActor.run { self.countries = fetched }
            } catch {}
        }
    }
    
    private func loadConsentText() {
        Task {
            do {
                let fetched = try await apiClient.fetchConsentText(language: localizationManager.currentLanguage)
                await MainActor.run { self.consentText = fetched.text }
            } catch {}
        }
    }
    
    private func submitApplication() {
        isLoading = true
        let application = AdmissionApplication(firstName: firstName, lastName: lastName, patronymic: patronymic.isEmpty ? nil : patronymic, phone: phone, email: email, dateOfBirth: dateOfBirth, placeOfBirth: placeOfBirth, nationality: nationality, passportNumber: passportNumber, passportIssue: passportIssue, passportExpiry: passportExpiry, visaCity: visaCity, program: program, comment: comment.isEmpty ? nil : comment)
        Task {
            do {
                _ = try await apiClient.submitAdmission(application)
                await MainActor.run { isLoading = false }
            } catch {
                await MainActor.run { isLoading = false }
            }
        }
    }
}

struct KleosTextFieldStyle: TextFieldStyle {
    func _body(configuration: TextField<Self._Label>) -> some View {
        configuration.padding().background(Color.white.opacity(0.1)).foregroundColor(.white).cornerRadius(8)
    }
}

struct ConsentTextView: View {
    let text: String
    @Environment(\.dismiss) var dismiss
    var body: some View {
        NavigationView {
            ScrollView { Text(text).padding().foregroundColor(.white) }
            .kleosBackground()
            .navigationTitle(LocalizationManager.shared.t("consent_text"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar { ToolbarItem(placement: .navigationBarTrailing) { Button(LocalizationManager.shared.t("close")) { dismiss() } } }
        }
    }
}
