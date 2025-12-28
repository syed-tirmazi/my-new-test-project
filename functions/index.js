const functions = require('firebase-functions');
const admin = require('firebase-admin');

admin.initializeApp();

exports.sendMessageNotification = functions.firestore
  .document('chats/{chatId}/messages/{messageId}')
  .onCreate(async (snap, context) => {
    const message = snap.data();
    const receiverId = message.receiverId;
    const senderId = message.senderId;

    if (!receiverId || !senderId) {
      console.log('Missing receiver or sender for notification');
      return null;
    }

    const receiverDoc = await admin.firestore().collection('users').doc(receiverId).get();
    const token = receiverDoc.get('fcmToken');
    if (!token) {
      console.log('Receiver has no token stored');
      return null;
    }

    const senderDoc = await admin.firestore().collection('users').doc(senderId).get();
    const senderName = senderDoc.get('displayName') || senderDoc.get('username') || 'New message';

    const payload = {
      notification: {
        title: senderName,
        body: message.body || 'You have a new message'
      },
      data: {
        senderName: senderName,
        body: message.body || ''
      }
    };

    await admin.messaging().sendToDevice(token, payload);
    return null;
  });
