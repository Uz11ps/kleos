import SwiftUI

struct ProgramsView: View {
    @StateObject private var apiClient = ApiClient.shared
    @StateObject private var localizationManager = LocalizationManager.shared
    @State private var programs: [Program] = []
    @State private var isLoading = false
    @State private var errorMessage: String? = nil
    @State private var showFilters = false
    @State private var filters = ProgramFilters(language: nil, level: nil, university: nil, universityId: nil, searchQuery: nil)
    
    var body: some View {
        ZStack {
            if isLoading {
                LoadingView()
            } else if let error = errorMessage, programs.isEmpty {
                VStack(spacing: 16) {
                    Text(t("error_loading_data"))
                        .foregroundColor(.gray)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal)
                    
                    Button(action: { loadPrograms() }) {
                        Text(t("retry"))
                            .fontWeight(.semibold)
                            .padding(.horizontal, 24)
                            .padding(.vertical, 10)
                            .background(Color.white.opacity(0.1))
                            .cornerRadius(20)
                    }
                }
                .frame(maxWidth: .infinity)
            } else {
                ScrollView {
                    VStack(alignment: .leading, spacing: 20) {
                        Color.clear.frame(height: 100)
                        
                        if programs.isEmpty {
                            VStack(spacing: 16) {
                                Text(t("no_programs_found")).foregroundColor(.gray).font(.headline)
                                Button(t("reset_filters")) {
                                    filters = ProgramFilters(language: nil, level: nil, university: nil, universityId: nil, searchQuery: nil)
                                    errorMessage = nil
                                    loadPrograms()
                                }.buttonStyle(KleosButtonStyle())
                            }
                            .frame(maxWidth: .infinity, minHeight: 300)
                        } else {
                            LazyVStack(spacing: 16) {
                                ForEach(programs) { program in
                                    NavigationLink(destination: ProgramDetailView(programId: program.id)) {
                                        ProgramCard(program: program)
                                    }
                                }
                            }
                            .padding(.horizontal, 24)
                        }
                        Color.clear.frame(height: 50)
                    }
                }
            }
        }
        .kleosBackground()
        .navigationTitle(t("programs"))
        .navigationBarTitleDisplayMode(.large)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button(action: { showFilters = true }) {
                    Image(systemName: "line.3.horizontal.decrease.circle").font(.title3).foregroundColor(.white)
                }
            }
        }
        .sheet(isPresented: $showFilters) {
            NavigationStack {
                ProgramsFiltersView(filters: $filters, onApply: {
                    showFilters = false
                    loadPrograms()
                })
                .navigationTitle(t("filters"))
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .navigationBarTrailing) {
                        Button(t("close")) { showFilters = false }
                    }
                }
            }
        }
        .task {
            if programs.isEmpty && !isLoading { loadPrograms() }
        }
        .onChange(of: localizationManager.currentLanguage) { _, _ in loadPrograms() }
    }
    
    private func loadPrograms() {
        guard !isLoading else { return }
        isLoading = true
        errorMessage = nil
        Task {
            do {
                let fetched = try await apiClient.fetchPrograms(filters: filters)
                await MainActor.run { self.programs = fetched; self.isLoading = false }
            } catch {
                await MainActor.run { 
                    self.isLoading = false 
                    self.errorMessage = error.localizedDescription
                }
            }
        }
    }
}

struct ProgramCard: View {
    let program: Program
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                CategoryBadge(text: program.level ?? LocalizationManager.shared.t("degree"), isInteresting: false)
                Spacer()
                if let duration = program.duration {
                    Text(duration).font(.system(size: 12, weight: .semibold)).foregroundColor(.gray)
                }
            }
            Text(program.name).font(.system(size: 20, weight: .bold)).foregroundColor(.white).lineLimit(2)
            if let uniName = program.universityName {
                HStack(spacing: 6) {
                    Image(systemName: "graduationcap").font(.system(size: 12))
                    Text(uniName).font(.system(size: 14))
                }
                .foregroundColor(.gray)
            }
            HStack {
                if let language = program.language {
                    HStack(spacing: 4) {
                        Image(systemName: "globe")
                        Text(language.uppercased())
                    }
                    .font(.system(size: 12, weight: .bold)).foregroundColor(Color.kleosPurple)
                }
                Spacer()
                Image(systemName: "arrow.right.circle.fill").font(.title2).foregroundColor(.white)
            }
        }
        .padding(20)
        .background(
            RoundedRectangle(cornerRadius: 24).fill(Color.white.opacity(0.08))
                .overlay(RoundedRectangle(cornerRadius: 24).stroke(Color.white.opacity(0.1), lineWidth: 1))
        )
    }
}

struct ProgramsFiltersView: View {
    @Binding var filters: ProgramFilters
    let onApply: () -> Void
    @StateObject private var apiClient = ApiClient.shared
    @State private var universities: [University] = []
    @State private var isLoading = true
    @State private var searchQuery = ""
    
    var body: some View {
        Form {
            Section(header: Text(LocalizationManager.shared.t("search")).foregroundColor(.gray)) {
                TextField(LocalizationManager.shared.t("search_programs"), text: $searchQuery)
                    .onChange(of: searchQuery) { filters.searchQuery = searchQuery.isEmpty ? nil : searchQuery }
            }
            Section(header: Text(LocalizationManager.shared.t("language")).foregroundColor(.gray)) {
                Picker(LocalizationManager.shared.t("select_language"), selection: Binding(
                    get: { filters.language ?? "" },
                    set: { filters.language = $0.isEmpty ? nil : $0 }
                )) {
                    Text(LocalizationManager.shared.t("all")).tag("")
                    Text("Russian").tag("ru")
                    Text("English").tag("en")
                    Text("Chinese").tag("zh")
                }
            }
            Section(header: Text(LocalizationManager.shared.t("level")).foregroundColor(.gray)) {
                Picker(LocalizationManager.shared.t("select_level"), selection: Binding(
                    get: { filters.level ?? "" },
                    set: { filters.level = $0.isEmpty ? nil : $0 }
                )) {
                    Text(LocalizationManager.shared.t("all")).tag("")
                    Text("Bachelor's degree").tag("Bachelor's degree")
                    Text("Master's degree").tag("Master's degree")
                    Text("Research degree").tag("Research degree")
                    Text("Speciality degree").tag("Speciality degree")
                    Text("Residency degree").tag("Residency degree")
                }
            }
            Section {
                Button(action: onApply) {
                    Text(LocalizationManager.shared.t("apply_filters")).frame(maxWidth: .infinity).fontWeight(.bold)
                }
                .listRowBackground(Color.kleosPurple).foregroundColor(.white)
            }
        }
        .task {
            do {
                let fetched = try await apiClient.fetchUniversities()
                await MainActor.run { self.universities = fetched; self.isLoading = false }
            } catch {
                await MainActor.run { self.isLoading = false }
            }
        }
    }
}

struct ProgramDetailView: View {
    let programId: String
    @StateObject private var apiClient = ApiClient.shared
    @State private var program: Program?
    @State private var isLoading = true
    @Environment(\.dismiss) var dismiss

    private var localizedLevel: String {
        guard let level = program?.level else { return "-" }
        switch level {
        case "Bachelor's degree": return "Бакалавриат"
        case "Master's degree": return "Магистратура"
        case "Research degree": return "Докторантура"
        case "Speciality degree": return "Специалитет"
        case "Residency degree": return "Ординатура"
        default: return level
        }
    }

    var body: some View {
        ZStack {
            if isLoading {
                LoadingView()
            } else if let program = program {
                ScrollView {
                    VStack(alignment: .leading, spacing: 18) {
                        Color.clear.frame(height: 110)
                        Text(program.title)
                            .font(.system(size: 30, weight: .bold))
                            .foregroundColor(.white)
                        Text(program.universityName ?? program.university ?? "")
                            .font(.system(size: 16))
                            .foregroundColor(.gray)

                        VStack(alignment: .leading, spacing: 10) {
                            detailRow("Language", program.language?.uppercased() ?? "-")
                            detailRow("Level", localizedLevel)
                            detailRow("Duration", program.duration ?? "-")
                            if let tuition = program.tuition, tuition > 0 {
                                detailRow("Tuition", "$\(Int(tuition)) / year")
                            }
                        }
                        .padding(16)
                        .background(Color.white.opacity(0.08))
                        .cornerRadius(16)

                        if let description = program.description, !description.isEmpty {
                            Text(description)
                                .font(.system(size: 16))
                                .foregroundColor(.white.opacity(0.92))
                                .lineSpacing(5)
                        }
                    }
                    .padding(.horizontal, 24)
                    .padding(.bottom, 40)
                }
            }
        }
        .kleosBackground().navigationBarTitleDisplayMode(.inline)
        .task {
            do {
                let fetched = try await apiClient.fetchProgramDetail(id: programId)
                await MainActor.run {
                    self.program = fetched
                    self.isLoading = false
                }
            } catch {
                await MainActor.run { self.isLoading = false }
            }
        }
    }

    @ViewBuilder
    private func detailRow(_ title: String, _ value: String) -> some View {
        HStack {
            Text(title).foregroundColor(.gray)
            Spacer()
            Text(value).foregroundColor(.white).fontWeight(.semibold)
        }
    }
}
