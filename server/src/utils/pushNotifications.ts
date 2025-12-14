import { User } from '../models/User.js';
import jwt from 'jsonwebtoken';

/**
 * Отправка push-уведомления через Firebase Cloud Messaging (FCM)
 * Поддерживает как Legacy API (через Server Key), так и новый метод (через OAuth2)
 */
async function sendPushNotification(fcmToken: string, title: string, body: string, data?: Record<string, string>): Promise<boolean> {
  if (!fcmToken || fcmToken.trim() === '') {
    console.error('FCM token is empty or invalid');
    return false;
  }
  
  const serverKey = process.env.FCM_SERVER_KEY;
  const projectId = process.env.FCM_PROJECT_ID || 'kleos-8e95f'; // Из google-services.json
  
  console.log(`Attempting to send push notification. Method: ${serverKey ? 'Legacy' : 'OAuth2'}, Project ID: ${projectId}`);
  
  // Используем новый метод через OAuth2, если Server Key не указан
  if (!serverKey) {
    console.log('FCM_SERVER_KEY not configured, using OAuth2 method...');
    return await sendPushNotificationOAuth2(fcmToken, title, body, data, projectId);
  }

  try {
    // Legacy метод через Server Key
    const response = await fetch('https://fcm.googleapis.com/fcm/send', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `key=${serverKey}`
      },
      body: JSON.stringify({
        to: fcmToken,
        notification: {
          title,
          body,
          sound: 'default',
          badge: '1'
        },
        data: data || {},
        priority: 'high'
      })
    });

    if (!response.ok) {
      const text = await response.text();
      console.error('FCM Legacy API error:', response.status, text);
      // Пробуем новый метод как fallback
      return await sendPushNotificationOAuth2(fcmToken, title, body, data, projectId);
    }

    const result = await response.json();
    if (result.failure === 1) {
      console.error('FCM send failed:', result.results);
      // Если токен невалидный, удаляем его из базы
      if (result.results?.[0]?.error === 'InvalidRegistration' || result.results?.[0]?.error === 'NotRegistered') {
        await User.updateOne({ fcmToken }, { $unset: { fcmToken: 1 } });
      }
      return false;
    }

    return true;
  } catch (error: any) {
    console.error('Error sending push notification (Legacy):', error);
    // Пробуем новый метод как fallback
    return await sendPushNotificationOAuth2(fcmToken, title, body, data, projectId);
  }
}

/**
 * Получение OAuth2 токена доступа для FCM HTTP v1 API через Service Account
 */
async function getAccessToken(): Promise<string | null> {
  let serviceAccount: any = null;
  
  // Пробуем сначала прочитать из файла (проще для настройки)
  const fs = await import('fs');
  const path = await import('path');
  const serviceAccountPath = process.env.FCM_SERVICE_ACCOUNT_PATH || path.join(process.cwd(), 'firebase-service-account.json');
  
  try {
    if (fs.existsSync(serviceAccountPath)) {
      const fileContent = fs.readFileSync(serviceAccountPath, 'utf-8');
      serviceAccount = JSON.parse(fileContent);
      console.log('Loaded service account from file:', serviceAccountPath);
    }
  } catch (e: any) {
    console.warn('Failed to read service account file:', e.message);
  }
  
  // Если файл не найден, пробуем из переменной окружения
  if (!serviceAccount) {
    const serviceAccountJson = process.env.FCM_SERVICE_ACCOUNT_JSON;
    if (serviceAccountJson) {
      try {
        serviceAccount = JSON.parse(serviceAccountJson);
        console.log('Loaded service account from environment variable');
      } catch (e: any) {
        console.warn('Failed to parse FCM_SERVICE_ACCOUNT_JSON:', e.message);
      }
    }
  }
  
  if (!serviceAccount) {
    console.warn('FCM Service Account not configured. Set FCM_SERVICE_ACCOUNT_PATH or FCM_SERVICE_ACCOUNT_JSON');
    return null;
  }

  try {
    const { private_key, client_email, project_id } = serviceAccount;
    
    if (!private_key || !client_email) {
      console.error('[OAuth2] Service account missing required fields: private_key or client_email');
      return null;
    }
    
    console.log(`[OAuth2] Creating JWT for client_email: ${client_email}`);
    
    // Создаем JWT для получения access token
    const now = Math.floor(Date.now() / 1000);
    
    const tokenPayload = {
      iss: client_email,
      scope: 'https://www.googleapis.com/auth/firebase.messaging',
      aud: 'https://oauth2.googleapis.com/token',
      exp: now + 3600,
      iat: now
    };
    
    console.log(`[OAuth2] Signing JWT with algorithm RS256...`);
    const token = jwt.sign(tokenPayload, private_key, { algorithm: 'RS256' });
    console.log(`[OAuth2] JWT created, length: ${token.length}`);

    // Обмениваем JWT на access token
    console.log(`[OAuth2] Exchanging JWT for access token...`);
    const response = await fetch('https://oauth2.googleapis.com/token', {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: new URLSearchParams({
        grant_type: 'urn:ietf:params:oauth:grant-type:jwt-bearer',
        assertion: token
      })
    });

    const responseText = await response.text();
    console.log(`[OAuth2] Token exchange response: ${response.status} ${response.statusText}`);
    
    if (!response.ok) {
      console.error(`[OAuth2] Failed to get access token: ${response.status}`);
      console.error(`[OAuth2] Response body: ${responseText}`);
      return null;
    }

    try {
      const result = JSON.parse(responseText);
      if (!result.access_token) {
        console.error('[OAuth2] Access token not found in response:', result);
        return null;
      }
      
      console.log(`[OAuth2] Access token obtained successfully, length: ${result.access_token.length}`);
      return result.access_token;
    } catch (e: any) {
      console.error('[OAuth2] Failed to parse token response:', e.message);
      console.error('[OAuth2] Raw response:', responseText.substring(0, 500));
      return null;
    }
  } catch (error: any) {
    console.error('[OAuth2] Error getting access token:', error.message);
    console.error('[OAuth2] Error stack:', error.stack);
    return null;
  }
}

/**
 * Отправка через новый FCM HTTP v1 API (не требует Legacy API)
 */
async function sendPushNotificationOAuth2(fcmToken: string, title: string, body: string, data?: Record<string, string>, projectId?: string): Promise<boolean> {
  if (!projectId) {
    console.error('FCM_PROJECT_ID not configured, skipping push notification');
    return false;
  }

  console.log(`Getting access token for project ${projectId}...`);
  // Получаем access token через Service Account
  const accessToken = await getAccessToken();
  if (!accessToken) {
    console.error('Failed to get access token, skipping push notification. Check FCM_SERVICE_ACCOUNT_PATH or FCM_SERVICE_ACCOUNT_JSON');
    return false;
  }
  
  console.log('Access token obtained successfully, length:', accessToken.length);

  try {
    // Используем новый FCM v1 API endpoint согласно документации Firebase
    const url = `https://fcm.googleapis.com/v1/projects/${projectId}/messages:send`;
    
    const messagePayload = {
      message: {
        token: fcmToken,
        notification: {
          title,
          body
        },
        data: data || {},
        android: {
          priority: 'high' as const,
          notification: {
            sound: 'default',
            channelId: 'default'
          }
        },
        apns: {
          headers: {
            'apns-priority': '10'
          },
          payload: {
            aps: {
              sound: 'default',
              badge: 1
            }
          }
        }
      }
    };
    
    console.log(`[FCM] Sending request to: ${url}`);
    console.log(`[FCM] Token length: ${fcmToken.length}, preview: ${fcmToken.substring(0, 30)}...`);
    console.log(`[FCM] Payload: ${JSON.stringify(messagePayload).substring(0, 200)}...`);
    
    const response = await fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${accessToken}`
      },
      body: JSON.stringify(messagePayload)
    });

    const responseText = await response.text();
    console.log(`FCM API response status: ${response.status} ${response.statusText}`);
    console.log(`FCM API response body: ${responseText.substring(0, 500)}`);
    
    if (!response.ok) {
      console.error(`FCM v1 API error: ${response.status} ${response.statusText}`);
      console.error('Full response body:', responseText);
      
      // Если токен невалидный, удаляем его из базы
      if (response.status === 404 || response.status === 400 || response.status === 403) {
        try {
          const errorData = JSON.parse(responseText);
          const errorMessage = errorData.error?.message || errorData.error || '';
          const errorCode = errorData.error?.code || '';
          console.error('FCM error details:', { message: errorMessage, code: errorCode });
          
          if (errorMessage.includes('Invalid') || 
              errorMessage.includes('not found') || 
              errorMessage.includes('registration') ||
              errorMessage.includes('token') ||
              errorCode === 'INVALID_ARGUMENT' ||
              errorCode === 'NOT_FOUND') {
            console.log(`Removing invalid FCM token from database (error: ${errorMessage})`);
            await User.updateOne({ fcmToken }, { $unset: { fcmToken: 1 } });
          }
        } catch (e) {
          console.error('Failed to parse error response:', e);
          console.error('Raw error response:', responseText);
        }
      }
      
      return false;
    }

    try {
      const result = JSON.parse(responseText);
      console.log('FCM v1 API success response:', JSON.stringify(result));
      
      if (result.name) {
        // Успешная отправка (v1 API возвращает объект с полем 'name')
        console.log('✅ Push notification sent successfully, message name:', result.name);
        return true;
      }

      console.warn('Unexpected FCM response format:', result);
      return false;
    } catch (e) {
      console.error('Failed to parse success response:', e);
      console.error('Raw success response:', responseText);
      return false;
    }
  } catch (error: any) {
    console.error('Error sending push notification (OAuth2):', error);
    return false;
  }
}

/**
 * Отправка push-уведомления конкретному пользователю
 */
export async function sendPushToUser(userId: string, title: string, body: string, data?: Record<string, string>): Promise<boolean> {
  const user = await User.findById(userId);
  if (!user || !(user as any).fcmToken) {
    return false;
  }

  return await sendPushNotification((user as any).fcmToken, title, body, data);
}

/**
 * Отправка push-уведомления всем пользователям с FCM токенами
 */
export async function sendPushToAll(title: string, body: string, data?: Record<string, string>): Promise<number> {
  const users = await User.find({ fcmToken: { $exists: true, $ne: null, $ne: '' } }).select('fcmToken email').lean();
  console.log(`Found ${users.length} users with FCM tokens`);
  if (users.length === 0) {
    console.log('No users with FCM tokens found. Users need to login and have FCM token registered.');
    // Выводим информацию о пользователях без токенов для отладки
    const totalUsers = await User.countDocuments();
    const usersWithoutTokens = await User.countDocuments({ $or: [{ fcmToken: { $exists: false } }, { fcmToken: null }, { fcmToken: '' }] });
    console.log(`Total users: ${totalUsers}, Users without FCM tokens: ${usersWithoutTokens}`);
    return 0;
  }

  let successCount = 0;
  // Отправляем уведомления параллельно, но с ограничением
  const batchSize = 10;
  for (let i = 0; i < users.length; i += batchSize) {
    const batch = users.slice(i, i + batchSize);
    const results = await Promise.allSettled(
      batch.map(async (user) => {
        const token = (user as any).fcmToken;
        const email = (user as any).email || 'unknown';
        console.log(`Sending notification to user ${email}, token: ${token?.substring(0, 20)}...`);
        try {
          const result = await sendPushNotification(token, title, body, data);
          if (!result) {
            console.error(`Failed to send notification to user ${email}`);
          }
          return result;
        } catch (error: any) {
          console.error(`Error sending notification to user ${email}:`, error.message);
          return false;
        }
      })
    );
    successCount += results.filter(r => r.status === 'fulfilled' && r.value === true).length;
    
    // Логируем ошибки
    results.forEach((result, index) => {
      if (result.status === 'rejected') {
        console.error(`Failed to send notification to user ${batch[index].email || 'unknown'}:`, result.reason);
      }
    });
  }

  console.log(`Sent push notifications: ${successCount}/${users.length} successful`);
  return successCount;
}

/**
 * Отправка push-уведомления всем пользователям с определенной ролью
 */
export async function sendPushToRole(role: string, title: string, body: string, data?: Record<string, string>): Promise<number> {
  const users = await User.find({ role, fcmToken: { $exists: true, $ne: null } }).select('fcmToken').lean();
  if (users.length === 0) {
    return 0;
  }

  let successCount = 0;
  const batchSize = 10;
  for (let i = 0; i < users.length; i += batchSize) {
    const batch = users.slice(i, i + batchSize);
    const results = await Promise.all(
      batch.map(user => sendPushNotification((user as any).fcmToken, title, body, data))
    );
    successCount += results.filter(r => r).length;
  }

  return successCount;
}

