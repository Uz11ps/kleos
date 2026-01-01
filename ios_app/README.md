# iOS App Structure

## Project Structure

```
ios_app/
├── Kleos/
│   ├── KleosApp.swift          # App entry point
│   ├── Models/
│   │   └── Models.swift        # All data models
│   ├── Network/
│   │   └── ApiClient.swift     # API client with all endpoints
│   ├── Utils/
│   │   ├── SessionManager.swift # User session management
│   │   └── UIUtils.swift        # UI utilities and styles
│   └── Views/
│       ├── SplashView.swift     # Splash and Onboarding
│       ├── AuthView.swift       # Login/Register
│       ├── MainTabView.swift    # Main navigation with TabView
│       ├── HomeView.swift       # Home screen with news
│       ├── NewsView.swift       # News list and detail
│       ├── UniversitiesView.swift # Universities list and detail
│       ├── ProgramsView.swift   # Programs filters and results
│       ├── GalleryView.swift    # Gallery grid and detail
│       ├── PartnersView.swift   # Partners list and detail
│       ├── ProfileView.swift    # User profile
│       ├── ChatView.swift       # Support chat with FAQ
│       └── AdmissionView.swift  # Admission application form
```

## Setup Instructions

1. **Create New Xcode Project:**
   - Open Xcode
   - Create new iOS App project
   - Name: `Kleos`
   - Interface: SwiftUI
   - Language: Swift
   - Bundle Identifier: `com.kleos.education`

2. **Add Files:**
   - Copy all files from `ios_app/Kleos/` to your Xcode project
   - Make sure to add them to the target

3. **Configure Info.plist:**
   - Add your API base URL if needed
   - Configure deep linking if using email verification

4. **Run:**
   - Select a simulator or device
   - Press Cmd+R to run

## Features Implemented

✅ Splash and Onboarding screens
✅ Authentication (Login/Register/Guest)
✅ Home screen with news filtering
✅ News list and detail views
✅ Universities list and detail views
✅ Programs with filters
✅ Gallery grid and detail views
✅ Partners list and detail views
✅ User profile with editing
✅ Support chat with FAQ
✅ Admission application form
✅ Session management
✅ API integration with all endpoints
✅ Dark theme matching Android design
✅ Navigation with TabView and Drawer menu
✅ Role-based access control

## API Configuration

Update `baseURL` in `ApiClient.swift`:
```swift
let baseURL = "https://api.kleos-study.ru"
```

## Notes

- All screens match the Android design with blurred circles background
- Navigation follows the same structure as Android app
- Guest users see only Home and Gallery tabs
- Registered users see University, Home, and Gallery tabs
- Drawer menu shows different items based on user role
- All API calls use async/await pattern
- Error handling is implemented throughout

