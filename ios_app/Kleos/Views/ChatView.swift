import SwiftUI

struct ChatView: View {
    @StateObject private var apiClient = ApiClient.shared
    @StateObject private var localizationManager = LocalizationManager.shared
    @State private var messages: [ChatMessage] = []
    @State private var faqItems: [FAQItem] = []
    @State private var newMessage = ""
    @State private var isLoading = true
    @State private var showFAQ = true
    
    var body: some View {
        ZStack {
            Color.kleosBackground.ignoresSafeArea()
            if showFAQ {
                FAQView(faqItems: $faqItems, onSendMessage: { question in
                    newMessage = question
                    showFAQ = false
                })
            } else {
                VStack(spacing: 0) {
                    ScrollViewReader { proxy in
                        ScrollView {
                            LazyVStack(spacing: 12) {
                                ForEach(messages) { message in
                                    ChatBubble(message: message).id(message.id)
                                }
                            }.padding()
                        }
                        .onChange(of: messages.count) { _, _ in
                            if let lastMessage = messages.last { withAnimation { proxy.scrollTo(lastMessage.id, anchor: .bottom) } }
                        }
                    }
                    HStack(spacing: 12) {
                        TextField(t("type_message"), text: $newMessage).textFieldStyle(.roundedBorder).onSubmit { sendMessage() }
                        Button(action: sendMessage) {
                            Image(systemName: "paperplane.fill").foregroundColor(.white).padding(12).background(Color.kleosPurple).clipShape(Circle())
                        }.disabled(newMessage.isEmpty)
                    }.padding().background(Color.kleosBackground)
                }
            }
        }
        .navigationTitle(t("support"))
        .navigationBarTitleDisplayMode(.large)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button(showFAQ ? t("chat") : t("faq")) { showFAQ.toggle() }
            }
        }
        .onAppear { loadData() }
        .onChange(of: localizationManager.currentLanguage) { _, _ in loadData() }
    }
    
    private func loadData() {
        loadFAQ()
        loadMessages()
    }
    
    private func loadFAQ() {
        let questions = [
            "When does application for fall intake open?", "Is there a spring intake?", "What documents are required to apply?",
            "Is legalization or apostille required?", "Is diploma recognition (nostrification) needed?", "What Russian level is required?",
            "Are there preparatory Russian courses?", "Are there entrance exams or interviews?", "How long does the visa invitation take?",
            "What visa type is issued first?", "How to pay tuition?", "Is a prepayment required?", "Refund if visa refused?",
            "Is dormitory provided?", "Dormitory cost?", "Is medical insurance required?", "Medical exam and fingerprints?",
            "Do I need migration registration?", "What is a migration card?", "Can I open a bank account?", "How to get a SIM card?",
            "Can I work while studying?", "Can I transfer from another university?", "Are scholarships available?",
            "Monthly living costs?", "Age limits?", "Is distance learning available?", "Multiple-entry visa immediately?",
            "Who translates documents into Russian?", "Can I pay in parts?"
        ]
        let answers = [
            "Usually opens in spring and lasts until July–August; check program dates.", "Some programs have February/March intake; confirm per program.", "Passport, diploma with transcript, notarized Russian translation, photo, motivation letter (if required).", "Yes, depending on your country; apostille or consular legalization is required.", "Sometimes; recognition may be required by the university or Rosobrnadzor (4–8 weeks).", "Typically TRKI/TORFL B1–B2 for Russian-taught programs; preparatory year possible.", "Yes, one-year preparatory program: Russian + profile subjects; internal exam at the end.", "Depends on program: often online interview and/or subject test.", "Usually 10–20 working days after payment and full documents.", "Usually single-entry student visa for 90 days, then extension and multiple-entry.", "Bank transfer by invoice; sometimes cards; bank fees paid by the student.", "Yes, often prepayment for a semester or a year is required.", "Usually partial refund minus admin fees; see contract terms.", "Yes, if places available; priority to first-year international students.", "On average 1,000–5,000 RUB per month depending on city and conditions.", "Yes, VMI policy required for the whole study period.", "Yes, within 90 days after entry: medical exam, photo and fingerprints.", "Yes, registration at place of stay within 3 working days; usually arranged by dorm/landlord.", "A form issued at the border; keep it until departure and for registration.", "Yes; usually passport, migration card, and registration are required.", "With passport at operator office; registration may be requested.", "Full-time international students can work, typically up to 20 h/week during term.", "Yes, with transcript and match to curriculum; admission office decides.", "Yes: Rossotrudnichestvo quotas and university grants; competitive.", "On average 20,000–40,000 RUB for food, transport, other expenses.", "Usually no strict limits for bachelor; minors need consent and guardian.", "For some programs yes; visa usually requires full-time.", "No; first single-entry 90 days, then extension to multiple-entry.", "Certified/Notarized translations accepted, in home country or in Russia.", "Often per semester; schedule is in the contract."
        ]
        faqItems = zip(questions, answers).enumerated().map { index, pair in
            FAQItem(id: "\(index + 1)", question: pair.0, answer: pair.1)
        }
    }
    
    private func loadMessages() {
        isLoading = true
        Task {
            do {
                let fetched = try await apiClient.fetchMessages()
                await MainActor.run { self.messages = fetched; self.isLoading = false }
            } catch {
                await MainActor.run { self.isLoading = false }
            }
        }
    }
    
    private func sendMessage() {
        guard !newMessage.isEmpty else { return }
        let messageText = newMessage.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !messageText.isEmpty else { return }
        newMessage = ""
        let tempMessage = ChatMessage(id: UUID().uuidString, text: messageText, senderRole: "student", createdAt: ISO8601DateFormatter().string(from: Date()))
        messages.append(tempMessage)
        Task {
            do {
                try await apiClient.sendMessage(text: messageText)
                loadMessages()
            } catch {
                if let index = messages.firstIndex(where: { $0.id == tempMessage.id }) { messages.remove(at: index) }
                newMessage = messageText
            }
        }
    }
}

struct ChatBubble: View {
    let message: ChatMessage
    var isUser: Bool { message.sender == "user" }
    
    var body: some View {
        HStack {
            if isUser { Spacer() }
            VStack(alignment: isUser ? .trailing : .leading, spacing: 4) {
                Text(message.text)
                    .padding(12)
                    .background(isUser ? Color.kleosPurple : Color.white.opacity(0.2))
                    .foregroundColor(.white)
                    .cornerRadius(16)
                
                Text(formatDate(message.createdAt))
                    .font(.system(size: 10))
                    .foregroundColor(.gray)
            }
            .frame(maxWidth: UIScreen.main.bounds.width * 0.7, alignment: isUser ? .trailing : .leading)
            if !isUser { Spacer() }
        }
    }
    
    private func formatDate(_ dateString: String) -> String {
        let isoFormatter = ISO8601DateFormatter()
        isoFormatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        var date = isoFormatter.date(from: dateString)
        if date == nil {
            isoFormatter.formatOptions = [.withInternetDateTime]
            date = isoFormatter.date(from: dateString)
        }
        if let date = date {
            let displayFormatter = DateFormatter()
            displayFormatter.timeStyle = .short
            displayFormatter.locale = Locale.current
            return displayFormatter.string(from: date)
        }
        return ""
    }
}

struct FAQView: View {
    @Binding var faqItems: [FAQItem]
    let onSendMessage: (String) -> Void
    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                Text(LocalizationManager.shared.t("interests_you")).font(.system(size: 24, weight: .bold)).foregroundColor(.white).padding()
                Button(action: { onSendMessage("") }) {
                    HStack { Text(LocalizationManager.shared.t("no_suitable_question")); Image(systemName: "arrow.right") }
                    .font(.system(size: 16, weight: .semibold)).foregroundColor(Color.kleosPinkAccent).frame(maxWidth: .infinity).padding().background(Color.white.opacity(0.1)).cornerRadius(12)
                }.padding(.horizontal)
                ForEach(faqItems.indices, id: \.self) { index in FAQItemView(item: $faqItems[index], onSendMessage: onSendMessage) }
            }
        }
    }
}

struct FAQItemView: View {
    @Binding var item: FAQItem
    let onSendMessage: (String) -> Void
    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Button(action: { withAnimation { item.isExpanded.toggle() } }) {
                HStack {
                    Text(item.question).font(.system(size: 16, weight: .semibold)).foregroundColor(.white).multilineTextAlignment(.leading)
                    Spacer()
                    Image(systemName: item.isExpanded ? "chevron.up" : "chevron.down").foregroundColor(.white)
                }.padding()
                .background(Color.white.opacity(0.1))
            }
            if item.isExpanded {
                VStack(alignment: .leading, spacing: 12) {
                    Text(item.answer).font(.system(size: 14)).foregroundColor(.gray).padding()
                    Button(action: { onSendMessage(item.question) }) {
                        Text(LocalizationManager.shared.t("send_this_question")).font(.system(size: 14, weight: .semibold)).foregroundColor(Color.kleosPinkAccent).frame(maxWidth: .infinity).padding(.vertical, 8).background(Color.white.opacity(0.1)).cornerRadius(8)
                    }.padding(.horizontal).padding(.bottom)
                }.background(Color.black.opacity(0.2))
            }
        }.cornerRadius(12).padding(.horizontal)
    }
}
