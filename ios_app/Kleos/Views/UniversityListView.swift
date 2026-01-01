import SwiftUI

struct University: Codable, Identifiable {
    let id: String
    let name: String
    let location: String
    let logoUrl: String?
    let description: String?
    
    enum CodingKeys: String, CodingKey {
        case id = "_id"
        case name, location, logoUrl, description
    }
}

struct UniversityListView: View {
    @State private var universities: [University] = []
    @State private var isLoading = true
    
    var body: some View {
        ZStack {
            Color.kleosBackground.ignoresSafeArea()
            
            // Фоновые круги
            VStack {
                HStack {
                    Spacer()
                    BlurredCircle(color: Color.blue.opacity(0.2))
                        .offset(x: 50, y: -50)
                }
                Spacer()
            }
            
            ScrollView {
                VStack(alignment: .leading, spacing: 20) {
                    Text("Universities")
                        .font(.system(size: 32, weight: .bold))
                        .foregroundColor(.white)
                        .padding(.horizontal, 24)
                    
                    if isLoading {
                        ProgressView().tint(.white)
                            .frame(maxWidth: .infinity, minHeight: 200)
                    } else {
                        LazyVStack(spacing: 16) {
                            ForEach(universities) { uni in
                                UniversityCard(university: uni)
                            }
                        }
                        .padding(.horizontal, 24)
                    }
                }
                .padding(.top, 20)
            }
        }
        .onAppear {
            loadUniversities()
        }
    }
    
    private func loadUniversities() {
        // Здесь должен быть вызов API. Пока создадим тестовые данные
        // В реальном приложении добавьте метод fetchUniversities в ApiClient
        self.universities = [
            University(id: "1", name: "Moscow State University", location: "Moscow, Russia", logoUrl: nil, description: "Leading university"),
            University(id: "2", name: "Saint Petersburg University", location: "St. Petersburg, Russia", logoUrl: nil, description: "Historic institution")
        ]
        self.isLoading = false
    }
}

struct UniversityCard: View {
    let university: University
    
    var body: some View {
        ZStack(alignment: .bottomLeading) {
            // Фон карточки (заглушка или фото)
            RoundedRectangle(cornerRadius: 20)
                .fill(Color.white.opacity(0.1))
                .frame(height: 180)
            
            VStack(alignment: .leading, spacing: 4) {
                Text("University")
                    .font(.system(size: 12, weight: .bold))
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(Color.blue)
                    .foregroundColor(.white)
                    .cornerRadius(4)
                
                Text(university.name)
                    .font(.system(size: 20, weight: .bold))
                    .foregroundColor(.white)
                
                HStack {
                    Image(systemName: "mappin.and.ellipse")
                    Text(university.location)
                }
                .font(.system(size: 14))
                .foregroundColor(.gray)
            }
            .padding(20)
            
            // Кнопка-стрелка справа
            VStack {
                Spacer()
                HStack {
                    Spacer()
                    Image(systemName: "arrow.up.right.circle.fill")
                        .resizable()
                        .frame(width: 32, height: 32)
                        .foregroundColor(.white)
                        .padding(20)
                }
            }
        }
    }
}

