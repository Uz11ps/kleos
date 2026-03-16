# Ответ для Apple App Review

## Текст ответа в App Store Connect

Hello Apple Review Team,

Thank you for your feedback. We have fixed both issues from the previous review.

1) Guideline 2.1 - App Completeness  
- We fixed the login flow where the app could remain in loading state after entering credentials.  
- The app now always exits loading state and shows either the main screen or an error message.

2) Guideline 2.3.3 - Accurate Metadata  
- We replaced all 13-inch iPad screenshots with real in-app UI screens showing the app in use (not promotional graphics, splash, or login-only screens).

Review account (already added in App Review Information):
- Email: demo@mail.ru
- Password: 123123

Please review the updated build and metadata.  
Thank you.

Best regards,  
Vladislav Vvedenskii

---

## Чеклист перед повторной отправкой

1. Проверка входа (критично для 2.1.0)
   - Удалить приложение с iPhone и iPad.
   - Установить чистую сборку TestFlight/Release.
   - Войти с `demo@mail.ru / 123123`.
   - Убедиться, что нет вечного лоадера: после входа открывается главный экран или показывается ошибка.

2. Что загружать в 13-inch iPad screenshots (критично для 2.3.3)
   - Home с контентом (лента и фильтры).
   - Universities list/detail.
   - Programs list.
   - Gallery list/detail.
   - Profile screen.
   - Не использовать splash/login/promotional-only кадры как большинство скриншотов.

3. App Review Information
   - Username: `demo@mail.ru`
   - Password: `123123`
   - Notes for reviewer: "Use this account to access all sections."

4. Перед нажатием Submit for Review
   - Проверить, что для iPad 13-inch действительно сохранены новые скриншоты через `View All Sizes in Media Manager`.
   - Проверить, что все обязательные локали показывают те же реальные экраны приложения.
