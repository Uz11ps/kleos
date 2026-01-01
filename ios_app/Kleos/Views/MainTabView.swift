import SwiftUI

struct MainTabView: View {
    @StateObject private var sessionManager = SessionManager.shared
    @State private var selectedTab = 0
    @State private var showDrawer = false
    
    var isGuest: Bool {
        sessionManager.isGuest()
    }
    
    var body: some View {
        ZStack {
            TabView(selection: $selectedTab) {
                // For guests: Home at 0, Gallery at 1
                // For users: University at 0, Home at 1, Gallery at 2
                
                if isGuest {
                    NavigationView {
                        HomeView()
                            .navigationBarTitleDisplayMode(.inline)
                            .toolbar {
                                ToolbarItem(placement: .navigationBarLeading) {
                                    Button(action: { showDrawer = true }) {
                                        Image(systemName: "line.3.horizontal")
                                            .foregroundColor(.white)
                                    }
                                }
                            }
                    }
                    .tabItem {
                        Label("Home", systemImage: "house.fill")
                    }
                    .tag(0)
                    
                    NavigationView {
                        GalleryView()
                    }
                    .tabItem {
                        Label("Gallery", systemImage: "photo.on.rectangle.angled")
                    }
                    .tag(1)
                } else {
                    NavigationView {
                        UniversitiesView()
                            .navigationBarTitleDisplayMode(.inline)
                            .toolbar {
                                ToolbarItem(placement: .navigationBarLeading) {
                                    Button(action: { showDrawer = true }) {
                                        Image(systemName: "line.3.horizontal")
                                            .foregroundColor(.white)
                                    }
                                }
                            }
                    }
                    .tabItem {
                        Label("University", systemImage: "graduationcap.fill")
                    }
                    .tag(0)
                    
                    NavigationView {
                        HomeView()
                    }
                    .tabItem {
                        Label("Home", systemImage: "house.fill")
                    }
                    .tag(1)
                    
                    NavigationView {
                        GalleryView()
                    }
                    .tabItem {
                        Label("Gallery", systemImage: "photo.on.rectangle.angled")
                    }
                    .tag(2)
                }
            }
            .accentColor(.blue)
            .onAppear {
                setupTabBarAppearance()
            }
            
            // Drawer Menu
            if showDrawer {
                DrawerMenuView(isPresented: $showDrawer, selectedTab: $selectedTab)
            }
        }
    }
    
    private func setupTabBarAppearance() {
        let appearance = UITabBarAppearance()
        appearance.configureWithOpaqueBackground()
        appearance.backgroundColor = UIColor(Color.kleosBackground)
        UITabBar.appearance().standardAppearance = appearance
        UITabBar.appearance().scrollEdgeAppearance = appearance
    }
}

struct DrawerMenuView: View {
    @Binding var isPresented: Bool
    @Binding var selectedTab: Int
    @StateObject private var sessionManager = SessionManager.shared
    
    var isGuest: Bool {
        sessionManager.isGuest()
    }
    
    var userRole: String? {
        sessionManager.getUserRole()
    }
    
    var body: some View {
        ZStack {
            Color.black.opacity(0.3)
                .ignoresSafeArea()
                .onTapGesture {
                    isPresented = false
                }
            
            HStack {
                VStack(alignment: .leading, spacing: 0) {
                    // Header
                    VStack(alignment: .leading, spacing: 8) {
                        Text(sessionManager.currentUser?.fullName ?? "Guest")
                            .font(.system(size: 24, weight: .bold))
                            .foregroundColor(.white)
                        
                        Text(sessionManager.currentUser?.email ?? "guest@local")
                            .font(.system(size: 14))
                            .foregroundColor(.gray)
                    }
                    .padding()
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(Color.kleosBackground)
                    
                    // Menu Items
                    ScrollView {
                        VStack(alignment: .leading, spacing: 0) {
                            if !isGuest {
                                DrawerMenuItem(icon: "person.fill", title: "Profile", action: {
                                    // Navigate to profile
                                    isPresented = false
                                })
                                
                                if userRole == "student" || userRole == "user" {
                                    DrawerMenuItem(icon: "doc.text.fill", title: "Admission", action: {
                                        // Navigate to admission
                                        isPresented = false
                                    })
                                }
                                
                                if userRole == "student" || userRole == "user" {
                                    DrawerMenuItem(icon: "message.fill", title: "Support", action: {
                                        // Navigate to chat
                                        isPresented = false
                                    })
                                }
                            }
                            
                            DrawerMenuItem(icon: "newspaper.fill", title: "News", action: {
                                // Navigate to news
                                isPresented = false
                            })
                            
                            DrawerMenuItem(icon: "book.fill", title: "Programs", action: {
                                // Navigate to programs
                                isPresented = false
                            })
                            
                            DrawerMenuItem(icon: "handshake.fill", title: "Partners", action: {
                                // Navigate to partners
                                isPresented = false
                            })
                            
                            if !isGuest {
                                Divider()
                                    .background(Color.white.opacity(0.2))
                                
                                DrawerMenuItem(icon: "arrow.right.square.fill", title: "Logout", action: {
                                    sessionManager.logout()
                                    isPresented = false
                                })
                            }
                        }
                    }
                    .background(Color.kleosBackground)
                }
                .frame(width: 280)
                .background(Color.kleosBackground)
                
                Spacer()
            }
        }
    }
}

struct DrawerMenuItem: View {
    let icon: String
    let title: String
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            HStack(spacing: 16) {
                Image(systemName: icon)
                    .foregroundColor(.white)
                    .frame(width: 24)
                
                Text(title)
                    .foregroundColor(.white)
                    .font(.system(size: 16))
                
                Spacer()
            }
            .padding()
        }
        .background(Color.white.opacity(0.05))
    }
}

