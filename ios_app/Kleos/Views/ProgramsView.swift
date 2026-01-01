import SwiftUI

struct ProgramsView: View {
    @StateObject private var apiClient = ApiClient.shared
    @State private var programs: [Program] = []
    @State private var isLoading = true
    @State private var showFilters = true
    @State private var filters = ProgramFilters(language: nil, educationLevel: nil, universityId: nil, searchQuery: nil)
    
    var body: some View {
        NavigationView {
            ZStack {
                Color.kleosBackground.ignoresSafeArea()
                
                if showFilters {
                    ProgramsFiltersView(filters: $filters, onApply: {
                        showFilters = false
                        loadPrograms()
                    })
                } else {
                    if isLoading {
                        LoadingView()
                    } else {
                        ScrollView {
                            LazyVStack(spacing: 16) {
                                ForEach(programs) { program in
                                    NavigationLink(destination: ProgramDetailView(programId: program.id)) {
                                        ProgramCard(program: program)
                                    }
                                }
                            }
                            .padding()
                        }
                        .toolbar {
                            ToolbarItem(placement: .navigationBarTrailing) {
                                Button("Filters") {
                                    showFilters = true
                                }
                            }
                        }
                    }
                }
            }
            .navigationTitle("Programs")
            .navigationBarTitleDisplayMode(.large)
            .onAppear {
                if !showFilters {
                    loadPrograms()
                }
            }
        }
    }
    
    private func loadPrograms() {
        isLoading = true
        
        Task {
            do {
                let fetched = try await apiClient.fetchPrograms(filters: filters)
                await MainActor.run {
                    self.programs = fetched
                    self.isLoading = false
                }
            } catch {
                await MainActor.run {
                    self.isLoading = false
                }
            }
        }
    }
}

struct ProgramsFiltersView: View {
    @Binding var filters: ProgramFilters
    let onApply: () -> Void
    @StateObject private var apiClient = ApiClient.shared
    @State private var universities: [University] = []
    @State private var searchQuery = ""
    
    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 24) {
                Text("Filters")
                    .font(.system(size: 32, weight: .bold))
                    .foregroundColor(.white)
                    .padding()
                
                VStack(alignment: .leading, spacing: 16) {
                    TextField("Search programs", text: $searchQuery)
                        .textFieldStyle(.roundedBorder)
                        .onChange(of: searchQuery) { newValue in
                            filters.searchQuery = newValue.isEmpty ? nil : newValue
                        }
                    
                    Picker("Language", selection: Binding(
                        get: { filters.language ?? "" },
                        set: { filters.language = $0.isEmpty ? nil : $0 }
                    )) {
                        Text("All").tag("")
                        Text("Russian").tag("Russian")
                        Text("English").tag("English")
                    }
                    .pickerStyle(.menu)
                    
                    Picker("Education Level", selection: Binding(
                        get: { filters.educationLevel ?? "" },
                        set: { filters.educationLevel = $0.isEmpty ? nil : $0 }
                    )) {
                        Text("All").tag("")
                        Text("Bachelor").tag("Bachelor")
                        Text("Master").tag("Master")
                        Text("PhD").tag("PhD")
                    }
                    .pickerStyle(.menu)
                    
                    Picker("University", selection: Binding(
                        get: { filters.universityId ?? "" },
                        set: { filters.universityId = $0.isEmpty ? nil : $0 }
                    )) {
                        Text("All").tag("")
                        ForEach(universities) { uni in
                            Text(uni.name).tag(uni.id)
                        }
                    }
                    .pickerStyle(.menu)
                }
                .padding()
                
                Button(action: onApply) {
                    Text("Find")
                        .fontWeight(.semibold)
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(KleosButtonStyle())
                .padding()
            }
        }
        .onAppear {
            loadUniversities()
        }
    }
    
    private func loadUniversities() {
        Task {
            do {
                let fetched = try await apiClient.fetchUniversities()
                await MainActor.run {
                    self.universities = fetched
                }
            } catch {
                // Handle error
            }
        }
    }
}

struct ProgramCard: View {
    let program: Program
    
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(program.name)
                .font(.system(size: 20, weight: .bold))
                .foregroundColor(.white)
            
            if let description = program.description {
                Text(description)
                    .font(.system(size: 14))
                    .foregroundColor(.gray)
                    .lineLimit(2)
            }
            
            HStack {
                if let university = program.universityName {
                    Text(university)
                        .font(.system(size: 14))
                        .foregroundColor(.gray)
                }
                
                Spacer()
                
                Image(systemName: "arrow.right")
                    .foregroundColor(.white)
            }
        }
        .padding()
        .background(Color.white.opacity(0.1))
        .cornerRadius(16)
    }
}

struct ProgramDetailView: View {
    let programId: String
    @Environment(\.dismiss) var dismiss
    
    var body: some View {
        Text("Program Detail")
            .foregroundColor(.white)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(action: { dismiss() }) {
                        Image(systemName: "arrow.left")
                            .foregroundColor(.white)
                    }
                }
            }
    }
}

