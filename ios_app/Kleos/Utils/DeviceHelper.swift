import SwiftUI
import UIKit

struct DeviceHelper {
    static var isIPad: Bool {
        UIDevice.current.userInterfaceIdiom == .pad
    }
    
    static var isIPhone: Bool {
        UIDevice.current.userInterfaceIdiom == .phone
    }
    
    // Адаптивный размер шрифта
    static func adaptiveFontSize(base: CGFloat) -> CGFloat {
        if isIPad {
            return base * 1.3
        }
        return base
    }
    
    // Адаптивный отступ
    static func adaptivePadding(base: CGFloat) -> CGFloat {
        if isIPad {
            return base * 1.5
        }
        return base
    }
    
    // Адаптивный размер кнопки (ширина)
    static func adaptiveButtonWidth(base: CGFloat) -> CGFloat {
        if isIPad {
            return base * 1.4
        }
        return base
    }
    
    // Адаптивный размер кнопки (высота)
    static func adaptiveButtonHeight(base: CGFloat) -> CGFloat {
        if isIPad {
            return base * 1.1
        }
        return base
    }
    
    // Адаптивный размер карточки (высота)
    static func adaptiveCardHeight(base: CGFloat) -> CGFloat {
        if isIPad {
            return base * 1.2
        }
        return base
    }
    
    // Максимальная ширина контента для iPad (центрирование)
    static func maxContentWidth(geometry: GeometryProxy) -> CGFloat {
        if isIPad {
            return min(geometry.size.width * 0.7, 800)
        }
        return geometry.size.width
    }
    
    // Количество колонок для сетки
    static func gridColumns() -> [GridItem] {
        if isIPad {
            return [
                GridItem(.flexible(), spacing: 20),
                GridItem(.flexible(), spacing: 20),
                GridItem(.flexible(), spacing: 20)
            ]
        }
        return [
            GridItem(.flexible(), spacing: 16),
            GridItem(.flexible(), spacing: 16)
        ]
    }
    
    // Ширина drawer menu
    static func drawerWidth() -> CGFloat {
        if isIPad {
            return 400
        }
        return 300
    }
    
    // Адаптивный размер аватара
    static func adaptiveAvatarSize(base: CGFloat) -> CGFloat {
        if isIPad {
            return base * 1.3
        }
        return base
    }
}
