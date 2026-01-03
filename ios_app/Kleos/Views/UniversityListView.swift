import SwiftUI

struct UniversityListView: View {
    @State private var universities: [University] = []
    @State private var isLoading = true
    @StateObject private var apiClient = ApiClient.shared
    
    var body: some View {
        ZStack {
            if isLoading {
                LoadingView()
            } else {
                ScrollView {
                    VStack(alignment: .leading, spacing: 20) {
                        Text("Universities")
                            .font(.system(size: 32, weight: .bold))
                            .foregroundColor(.white)
                            .padding(.horizontal, 24)
                        
                        LazyVStack(spacing: 16) {
                            ForEach(universities) { uni in
                                UniversityCard(university: uni)
                            }
                        }
                        .padding(.horizontal, 24)
                    }
                    .padding(.top, 20)
                }
            }
        }
        .kleosBackground()
        .task {
            await loadUniversities()
        }
    }
    
    private func loadUniversities() async {
        do {
            let fetched = try await apiClient.fetchUniversities()
            await MainActor.run {
                self.universities = fetched
                self.isLoading = false
            }
        } catch {
            print("❌ Error loading universities: \(error)")
            await MainActor.run {
                self.isLoading = false
            }
        }
    }
}
