import SwiftUI

struct ChatView: View {
    @StateObject private var apiClient = ApiClient.shared
    @State private var messages: [ChatMessage] = []
    @State private var faqItems: [FAQItem] = []
    @State private var newMessage = ""
    @State private var isLoading = true
    @State private var showFAQ = true
    
    var body: some View {
        NavigationView {
            ZStack {
                Color.kleosBackground.ignoresSafeArea()
                
                if showFAQ {
                    FAQView(faqItems: $faqItems, onSendMessage: { question in
                        newMessage = question
                        showFAQ = false
                    })
                } else {
                    VStack(spacing: 0) {
                        // Messages
                        ScrollViewReader { proxy in
                            ScrollView {
                                LazyVStack(spacing: 12) {
                                    ForEach(messages) { message in
                                        ChatBubble(message: message)
                                            .id(message.id)
                                    }
                                }
                                .padding()
                            }
                            .onChange(of: messages.count) { _ in
                                if let lastMessage = messages.last {
                                    withAnimation {
                                        proxy.scrollTo(lastMessage.id, anchor: .bottom)
                                    }
                                }
                            }
                        }
                        
                        // Input
                        HStack(spacing: 12) {
                            TextField("Type a message...", text: $newMessage)
                                .textFieldStyle(.roundedBorder)
                                .onSubmit {
                                    sendMessage()
                                }
                            
                            Button(action: sendMessage) {
                                Image(systemName: "paperplane.fill")
                                    .foregroundColor(.white)
                                    .padding(12)
                                    .background(Color.blue)
                                    .clipShape(Circle())
                            }
                            .disabled(newMessage.isEmpty)
                        }
                        .padding()
                        .background(Color.kleosBackground)
                    }
                }
            }
            .navigationTitle("Support")
            .navigationBarTitleDisplayMode(.large)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(showFAQ ? "Chat" : "FAQ") {
                        showFAQ.toggle()
                    }
                }
            }
            .onAppear {
                loadData()
            }
        }
    }
    
    private func loadData() {
        loadFAQ()
        loadMessages()
    }
    
    private func loadFAQ() {
        // FAQ items - можно загрузить с сервера или использовать статичные
        faqItems = [
            FAQItem(id: "1", question: "When does application for fall intake open?", answer: "Usually opens in spring and lasts until July–August; check program dates."),
            FAQItem(id: "2", question: "Is there a spring intake?", answer: "Some programs have February/March intake; confirm per program."),
            FAQItem(id: "3", question: "What documents are required to apply?", answer: "Passport, diploma with transcript, notarized Russian translation, photo, motivation letter (if required).")
        ]
    }
    
    private func loadMessages() {
        isLoading = true
        
        Task {
            do {
                let fetched = try await apiClient.fetchMessages()
                await MainActor.run {
                    self.messages = fetched
                    self.isLoading = false
                }
            } catch {
                await MainActor.run {
                    self.isLoading = false
                }
            }
        }
    }
    
    private func sendMessage() {
        guard !newMessage.isEmpty else { return }
        
        let messageText = newMessage
        newMessage = ""
        
        Task {
            do {
                let sent = try await apiClient.sendMessage(text: messageText)
                await MainActor.run {
                    self.messages.append(sent)
                }
                loadMessages() // Reload to get response
            } catch {
                // Handle error
            }
        }
    }
}

struct ChatBubble: View {
    let message: ChatMessage
    
    var isUser: Bool {
        message.sender == "user"
    }
    
    var body: some View {
        HStack {
            if isUser {
                Spacer()
            }
            
            VStack(alignment: isUser ? .trailing : .leading, spacing: 4) {
                Text(message.text)
                    .padding(12)
                    .background(isUser ? Color.blue : Color.white.opacity(0.2))
                    .foregroundColor(isUser ? .white : .white)
                    .cornerRadius(16)
                
                Text(formatDate(message.createdAt))
                    .font(.caption2)
                    .foregroundColor(.gray)
            }
            .frame(maxWidth: UIScreen.main.bounds.width * 0.7, alignment: isUser ? .trailing : .leading)
            
            if !isUser {
                Spacer()
            }
        }
    }
    
    private func formatDate(_ dateString: String) -> String {
        let formatter = ISO8601DateFormatter()
        if let date = formatter.date(from: dateString) {
            let displayFormatter = DateFormatter()
            displayFormatter.timeStyle = .short
            return displayFormatter.string(from: date)
        }
        return dateString
    }
}

struct FAQView: View {
    @Binding var faqItems: [FAQItem]
    let onSendMessage: (String) -> Void
    
    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                Text("What interests you?")
                    .font(.system(size: 24, weight: .bold))
                    .foregroundColor(.white)
                    .padding()
                
                ForEach(faqItems.indices, id: \.self) { index in
                    FAQItemView(item: $faqItems[index], onSendMessage: onSendMessage)
                }
                
                Button(action: {
                    onSendMessage("")
                }) {
                    HStack {
                        Text("No suitable question? Submit a request")
                        Image(systemName: "arrow.right")
                    }
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundColor(.blue)
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(Color.white.opacity(0.1))
                    .cornerRadius(12)
                }
                .padding()
            }
        }
    }
}

struct FAQItemView: View {
    @Binding var item: FAQItem
    let onSendMessage: (String) -> Void
    
    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Button(action: {
                withAnimation {
                    item.isExpanded.toggle()
                }
            }) {
                HStack {
                    Text(item.question)
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundColor(.white)
                        .multilineTextAlignment(.leading)
                    
                    Spacer()
                    
                    Image(systemName: item.isExpanded ? "chevron.up" : "chevron.down")
                        .foregroundColor(.white)
                }
                .padding()
                .background(Color.white.opacity(0.1))
            }
            
            if item.isExpanded {
                VStack(alignment: .leading, spacing: 12) {
                    Text(item.answer)
                        .font(.system(size: 14))
                        .foregroundColor(.gray)
                        .padding()
                    
                    Button(action: {
                        onSendMessage(item.question)
                    }) {
                        Text("Send this question")
                            .font(.system(size: 14, weight: .semibold))
                            .foregroundColor(.blue)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 8)
                            .background(Color.white.opacity(0.1))
                            .cornerRadius(8)
                    }
                    .padding(.horizontal)
                    .padding(.bottom)
                }
                .background(Color.black.opacity(0.2))
            }
        }
        .cornerRadius(12)
        .padding(.horizontal)
    }
}

