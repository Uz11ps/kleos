import Foundation
import SwiftUI
import Combine

class LocalizationManager: ObservableObject {
    static let shared = LocalizationManager()
    
    @AppStorage("selectedLanguage") var currentLanguage: String = "en" {
        didSet {
            loadTranslations()
        }
    }
    
    @Published var translations: [String: String] = [:]
    @Published var isLoading: Bool = false
    
    private let fallbacks: [String: [String: String]] = [
        "ru": [
            "home": "Главная",
            "gallery": "Галерея",
            "universities": "Университеты",
            "news": "Новости",
            "programs": "Программы",
            "partners": "Партнеры",
            "profile": "Профиль",
            "admission": "Заявка",
            "support": "Поддержка",
            "logout": "Выйти",
            "guest": "Гость",
            "welcome_back": "С возвращением,",
            "explore_new_areas": "Давайте исследуем новые области",
            "all": "Все",
            "interesting": "Интересное",
            "no_content": "Нет контента",
            "contacts": "Контакты",
            "website": "Сайт",
            "open_website": "Открыть сайт",
            "no_news": "Нет новостей",
            "video_player": "Видео плеер",
            "no_programs_found": "Программы не найдены",
            "reset_filters": "Сбросить фильтры",
            "filters": "Фильтры",
            "close": "Закрыть",
            "search": "Поиск",
            "search_programs": "Поиск программ",
            "language": "Язык",
            "select_language": "Выберите язык",
            "level": "Уровень",
            "select_level": "Выберите уровень",
            "apply_filters": "Применить фильтры",
            "degree": "Степень",
            "first_name": "Имя",
            "last_name": "Фамилия",
            "patronymic": "Отчество",
            "phone": "Телефон",
            "email": "Email",
            "date_of_birth": "Дата рождения",
            "place_of_birth": "Место рождения",
            "nationality": "Гражданство",
            "select": "Выбрать",
            "passport_number": "Номер паспорта",
            "passport_issue": "Кем и когда выдан",
            "passport_expiry": "Срок действия",
            "visa_city": "Город получения визы",
            "program": "Программа",
            "comment": "Комментарий",
            "agree_processing": "Я согласен на обработку данных",
            "read_full_text": "Читать текст",
            "submit_application": "Отправить заявку",
            "consent_text": "Текст согласия",
            "chat": "Чат",
            "faq": "FAQ",
            "type_message": "Введите сообщение...",
            "interests_you": "Что вас интересует?",
            "no_suitable_question": "Нет подходящего вопроса?",
            "send_this_question": "Отправить этот вопрос",
            "welcome": "Добро пожаловать",
            "auth_description": "Войдите или создайте профиль",
            "sign_in": "Войти",
            "sign_up": "Регистрация",
            "login_as_guest": "Или войти как гость",
            "enter_email": "Введите email",
            "enter_password": "Введите пароль",
            "login_error": "Ошибка входа",
            "enter_full_name": "Введите ФИО",
            "register_error": "Ошибка регистрации",
            "verify_email": "Подтвердите Email",
            "verify_email_description": "Мы отправили письмо с подтверждением. Откройте ссылку из письма на этом устройстве.",
            "address": "Адрес",
            "full_name": "ФИО",
            "password": "Пароль",
            "study": "Изучайте",
            "programs_and": "программы и",
            "submit_apps": "подавайте заявки"
        ],
        "en": [
            "home": "Home",
            "gallery": "Gallery",
            "universities": "Universities",
            "news": "News",
            "programs": "Programs",
            "partners": "Partners",
            "profile": "Profile",
            "admission": "Admission",
            "support": "Support",
            "logout": "Logout",
            "guest": "Guest",
            "welcome_back": "Welcome back,",
            "explore_new_areas": "Let's explore new areas",
            "all": "All",
            "interesting": "Interesting",
            "no_content": "No content",
            "contacts": "Contacts",
            "website": "Website",
            "open_website": "Open website",
            "no_news": "No news",
            "video_player": "Video Player",
            "no_programs_found": "No programs found",
            "reset_filters": "Reset Filters",
            "filters": "Filters",
            "close": "Close",
            "search": "Search",
            "search_programs": "Search programs",
            "language": "Language",
            "select_language": "Select Language",
            "level": "Level",
            "select_level": "Select Level",
            "apply_filters": "Apply Filters",
            "degree": "Degree",
            "first_name": "First Name",
            "last_name": "Last Name",
            "patronymic": "Patronymic",
            "phone": "Phone",
            "email": "Email",
            "date_of_birth": "Date of Birth",
            "place_of_birth": "Place of Birth",
            "nationality": "Nationality",
            "select": "Select",
            "passport_number": "Passport Number",
            "passport_issue": "Passport Issue",
            "passport_expiry": "Passport Expiry",
            "visa_city": "Visa City",
            "program": "Program",
            "comment": "Comment",
            "agree_processing": "I agree to data processing",
            "read_full_text": "Read text",
            "submit_application": "Submit Application",
            "consent_text": "Consent Text",
            "chat": "Chat",
            "faq": "FAQ",
            "type_message": "Type a message...",
            "interests_you": "What interests you?",
            "no_suitable_question": "No suitable question?",
            "send_this_question": "Send this question",
            "welcome": "Welcome",
            "auth_description": "Log in or create a profile",
            "sign_in": "Sign In",
            "sign_up": "Sign Up",
            "login_as_guest": "Or login as guest",
            "enter_email": "Enter email",
            "enter_password": "Enter password",
            "login_error": "Login error",
            "enter_full_name": "Enter full name",
            "register_error": "Registration error",
            "verify_email": "Verify Email",
            "verify_email_description": "We sent an email with a verification link. Open the link from the email on this device.",
            "address": "Address",
            "full_name": "Full Name",
            "password": "Password",
            "study": "Study",
            "programs_and": "programs and",
            "submit_apps": "submit applications"
        ],
        "zh": [
            "home": "首页",
            "gallery": "画廊",
            "universities": "大学",
            "news": "新闻",
            "programs": "项目",
            "partners": "合作伙伴",
            "profile": "个人资料",
            "admission": "入学申请",
            "support": "支持",
            "logout": "登出",
            "guest": "游客",
            "welcome_back": "欢迎回来，",
            "explore_new_areas": "让我们探索新领域",
            "all": "全部",
            "interesting": "有趣",
            "no_content": "无内容",
            "contacts": "联系方式",
            "website": "网站",
            "open_website": "打开网站",
            "no_news": "暂无新闻",
            "video_player": "视频播放器",
            "no_programs_found": "未找到项目",
            "reset_filters": "重置过滤器",
            "filters": "过滤器",
            "close": "关闭",
            "search": "搜索",
            "search_programs": "搜索项目",
            "language": "语言",
            "select_language": "选择语言",
            "level": "级别",
            "select_level": "选择级别",
            "apply_filters": "应用过滤器",
            "degree": "学位",
            "first_name": "名字",
            "last_name": "姓氏",
            "patronymic": "父名",
            "phone": "电话",
            "email": "电子邮件",
            "date_of_birth": "出生日期",
            "place_of_birth": "出生地点",
            "nationality": "国籍",
            "select": "选择",
            "passport_number": "护照号码",
            "passport_issue": "签发机关",
            "passport_expiry": "有效期",
            "visa_city": "签证城市",
            "program": "项目",
            "comment": "备注",
            "agree_processing": "我同意数据处理",
            "read_full_text": "阅读全文",
            "submit_application": "提交申请",
            "consent_text": "同意书内容",
            "chat": "聊天",
            "faq": "常见问题",
            "type_message": "输入消息...",
            "interests_you": "你对什么感兴趣？",
            "no_suitable_question": "没有合适的问题？",
            "send_this_question": "发送此问题",
            "welcome": "欢迎",
            "auth_description": "登录或创建个人资料",
            "sign_in": "登录",
            "sign_up": "注册",
            "login_as_guest": "或作为游客登录",
            "enter_email": "输入电子邮件",
            "enter_password": "输入密码",
            "login_error": "登录错误",
            "enter_full_name": "输入全名",
            "register_error": "注册错误",
            "verify_email": "验证电子邮件",
            "verify_email_description": "我们发送了一封包含验证链接的电子邮件。请在设备上打开邮件中的链接。",
            "address": "地址",
            "full_name": "全名",
            "password": "密码",
            "study": "研究",
            "programs_and": "项目和",
            "submit_apps": "提交申请"
        ]
    ]
    
    private init() {
        loadTranslations()
    }
    
    func loadTranslations() {
        isLoading = true
        Task {
            do {
                let fetched = try await ApiClient.shared.fetchTranslations(language: currentLanguage)
                await MainActor.run {
                    self.translations = fetched
                    self.isLoading = false
                }
            } catch {
                print("❌ Error loading translations: \(error)")
                await MainActor.run {
                    self.isLoading = false
                }
            }
        }
    }
    
    func t(_ key: String) -> String {
        return translations[key] ?? fallbacks[currentLanguage]?[key] ?? key
    }
}

extension View {
    func t(_ key: String) -> String {
        LocalizationManager.shared.t(key)
    }
}

