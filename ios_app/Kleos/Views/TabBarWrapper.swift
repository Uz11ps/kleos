import SwiftUI
import UIKit

// Environment key для drawer toggle
struct DrawerToggleKey: EnvironmentKey {
    static let defaultValue: () -> Void = {}
}

extension EnvironmentValues {
    var drawerToggle: () -> Void {
        get { self[DrawerToggleKey.self] }
        set { self[DrawerToggleKey.self] = newValue }
    }
}

struct TabBarWrapper: UIViewControllerRepresentable {
    @Binding var selectedTab: Int
    let isGuest: Bool
    let onDrawerToggle: () -> Void
    
    func makeUIViewController(context: Context) -> UITabBarController {
        let tabBarController = UITabBarController()
        
        // КРИТИЧНО: Отключаем split-view на iPad
        tabBarController.modalPresentationStyle = .fullScreen
        
        // Дополнительные настройки для предотвращения split-view
        if UIDevice.current.userInterfaceIdiom == .pad {
            tabBarController.preferredDisplayMode = .oneBesideSecondary
            // Принудительно устанавливаем размер для full screen
            if let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene {
                tabBarController.view.frame = windowScene.coordinateSpace.bounds
            }
        }
        
        var viewControllers: [UIViewController] = []
        
        // Создаем обертку для передачи onDrawerToggle через Environment
        let drawerToggleWrapper = { onDrawerToggle() }
        
        if isGuest {
            let homeView = HomeView().environment(\.drawerToggle, drawerToggleWrapper)
            let homeVC = UIHostingController(rootView: homeView)
            homeVC.tabBarItem = UITabBarItem(title: LocalizationManager.shared.t("home"), image: UIImage(systemName: "house.fill"), tag: 0)
            viewControllers.append(homeVC)
            
            let galleryView = GalleryView().environment(\.drawerToggle, drawerToggleWrapper)
            let galleryVC = UIHostingController(rootView: galleryView)
            galleryVC.tabBarItem = UITabBarItem(title: LocalizationManager.shared.t("gallery"), image: UIImage(systemName: "photo.on.rectangle.angled"), tag: 1)
            viewControllers.append(galleryVC)
        } else {
            let homeView = HomeView().environment(\.drawerToggle, drawerToggleWrapper)
            let homeVC = UIHostingController(rootView: homeView)
            homeVC.tabBarItem = UITabBarItem(title: LocalizationManager.shared.t("home"), image: UIImage(systemName: "house.fill"), tag: 0)
            viewControllers.append(homeVC)
            
            let universitiesView = UniversitiesView().environment(\.drawerToggle, drawerToggleWrapper)
            let universitiesVC = UIHostingController(rootView: universitiesView)
            universitiesVC.tabBarItem = UITabBarItem(title: LocalizationManager.shared.t("universities"), image: UIImage(systemName: "graduationcap.fill"), tag: 1)
            viewControllers.append(universitiesVC)
            
            let galleryView = GalleryView().environment(\.drawerToggle, drawerToggleWrapper)
            let galleryVC = UIHostingController(rootView: galleryView)
            galleryVC.tabBarItem = UITabBarItem(title: LocalizationManager.shared.t("gallery"), image: UIImage(systemName: "photo.on.rectangle.angled"), tag: 2)
            viewControllers.append(galleryVC)
        }
        
        tabBarController.viewControllers = viewControllers
        tabBarController.selectedIndex = selectedTab
        
        // Настройка внешнего вида
        let appearance = UITabBarAppearance()
        appearance.configureWithTransparentBackground()
        appearance.backgroundColor = .clear
        tabBarController.tabBar.standardAppearance = appearance
        tabBarController.tabBar.scrollEdgeAppearance = appearance
        
        return tabBarController
    }
    
    func updateUIViewController(_ uiViewController: UITabBarController, context: Context) {
        uiViewController.selectedIndex = selectedTab
    }
}
