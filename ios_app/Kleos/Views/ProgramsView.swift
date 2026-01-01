import SwiftUI

struct ProgramsView: View {
    @StateObject private var apiClient = ApiClient.shared
    @State private var programs: [Program] = []
    @State private var isLoading = true
    @State private var showFilters = true
    @State private var filters = ProgramFilters(language: nil, level: nil, university: nil, universityId: nil, searchQuery: nil)
    
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
    @State private var languages: [String] = []
    @State private var levels: [String] = []
    @State private var searchQuery = ""
    @State private var isLoading = true
    
    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 24) {
                Text("Filters")
                    .font(.system(size: 32, weight: .bold))
                    .foregroundColor(.white)
                    .padding()
                
                if isLoading {
                    ProgressView()
                        .frame(maxWidth: .infinity, alignment: .center)
                        .padding()
                } else {
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
                            ForEach(languages, id: \.self) { lang in
                                Text(lang).tag(lang)
                            }
                        }
                        .pickerStyle(.menu)
                        
                        Picker("Education Level", selection: Binding(
                            get: { filters.level ?? "" },
                            set: { filters.level = $0.isEmpty ? nil : $0 }
                        )) {
                            Text("All").tag("")
                            ForEach(levels, id: \.self) { level in
                                Text(level).tag(level)
                            }
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
        }
        .onAppear {
            loadFilterOptions()
        }
    }
    
    private func loadFilterOptions() {
        isLoading = true
        
        Task {
            do {
                // Загружаем все данные параллельно
                async let universitiesTask = apiClient.fetchUniversities()
                async let programsTask = apiClient.fetchPrograms(filters: nil)
                
                let (fetchedUniversities, allPrograms) = try await (universitiesTask, programsTask)
                
                // Извлекаем уникальные языки и уровни из программ
                let uniqueLanguages = Set(allPrograms.compactMap { $0.language }).sorted()
                let uniqueLevels = Set(allPrograms.compactMap { $0.level }).sorted()
                
                await MainActor.run {
                    self.universities = fetchedUniversities
                    self.languages = uniqueLanguages
                    self.levels = uniqueLevels
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

struct ProgramCard: View {
    let program: Program
    
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(program.title)
                .font(.system(size: 20, weight: .bold))
                .foregroundColor(.white)
            
            if let description = program.description {
                Text(description)
                    .font(.system(size: 14))
                    .foregroundColor(.gray)
                    .lineLimit(3)
            }
            
            HStack(spacing: 16) {
                if let language = program.language {
                    HStack(spacing: 4) {
                        Image(systemName: "globe")
                            .font(.system(size: 12))
                        Text(language)
                            .font(.system(size: 12))
                    }
                    .foregroundColor(.gray)
                }
                
                if let level = program.level {
                    HStack(spacing: 4) {
                        Image(systemName: "graduationcap")
                            .font(.system(size: 12))
                        Text(level)
                            .font(.system(size: 12))
                    }
                    .foregroundColor(.gray)
                }
                
                if let tuition = program.tuition {
                    HStack(spacing: 4) {
                        Image(systemName: "dollarsign.circle")
                            .font(.system(size: 12))
                        Text(String(format: "%.0f", tuition))
                            .font(.system(size: 12))
                    }
                    .foregroundColor(.gray)
                }
            }
            
            HStack {
                if let university = program.university {
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
    @StateObject private var apiClient = ApiClient.shared
    @State private var program: Program?
    @State private var isLoading = true
    @State private var errorMessage: String?
    
    var body: some View {
        ZStack {
            Color.kleosBackground.ignoresSafeArea()
            
            if isLoading {
                ProgressView()
                    .tint(.white)
            } else if let program = program {
                ScrollView {
                    VStack(alignment: .leading, spacing: 24) {
                        // Title
                        Text(program.title)
                            .font(.system(size: 32, weight: .bold))
                            .foregroundColor(.white)
                        
                        // Description
                        if let description = program.description {
                            Text(description)
                                .font(.system(size: 16))
                                .foregroundColor(.gray)
                        }
                        
                        // Details
                        VStack(alignment: .leading, spacing: 16) {
                            if let language = program.language {
                                DetailRow(icon: "globe", title: "Language", value: language)
                            }
                            
                            if let level = program.level {
                                DetailRow(icon: "graduationcap", title: "Level", value: level)
                            }
                            
                            if let duration = program.duration {
                                DetailRow(icon: "clock", title: "Duration", value: duration)
                            }
                            
                            if let tuition = program.tuition {
                                DetailRow(icon: "dollarsign.circle", title: "Tuition", value: String(format: "%.0f", tuition))
                            }
                            
                            if let university = program.university {
                                DetailRow(icon: "building.2", title: "University", value: university)
                            }
                        }
                        .padding()
                        .background(Color.white.opacity(0.1))
                        .cornerRadius(16)
                    }
                    .padding()
                }
            } else if let error = errorMessage {
                VStack {
                    Text("Error loading program")
                        .foregroundColor(.red)
                    Text(error)
                        .foregroundColor(.gray)
                        .font(.system(size: 14))
                }
            }
        }
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .navigationBarLeading) {
                Button(action: { dismiss() }) {
                    Image(systemName: "arrow.left")
                        .foregroundColor(.white)
                }
            }
        }
        .onAppear {
            loadProgram()
        }
    }
    
    private func loadProgram() {
        isLoading = true
        errorMessage = nil
        
        Task {
            do {
                let fetched = try await apiClient.fetchProgram(id: programId)
                await MainActor.run {
                    self.program = fetched
                    self.isLoading = false
                }
            } catch {
                await MainActor.run {
                    self.errorMessage = error.localizedDescription
                    self.isLoading = false
                }
            }
        }
    }
}

struct DetailRow: View {
    let icon: String
    let title: String
    let value: String
    
    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: icon)
                .foregroundColor(.white)
                .frame(width: 24)
            
            Text(title)
                .foregroundColor(.gray)
            
            Spacer()
            
            Text(value)
                .foregroundColor(.white)
                .fontWeight(.semibold)
        }
    }
}

