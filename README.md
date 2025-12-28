# Chatter

A Jetpack Compose Android chat sample using Firebase Authentication, Cloud Firestore, and Firebase Cloud Messaging. Users can register a username, search for other users by username, exchange messages, and receive push notifications when a new message arrives.

## Features
- Anonymous Firebase Authentication with lightweight profile setup (username + display name).
- Username search powered by Firestore range queries.
- Realtime chat threads stored in `chats/{chatId}/messages` with conversation summaries.
- FCM token management in the user profile for targeted message notifications.
- Firebase Cloud Functions trigger that sends push notifications to the receiver when a message is created.
- Jetpack Compose UI with navigation between profile setup, search, and chat screens.

## Project structure
- `app/` — Android application code.
  - `data/` — Firebase repositories for users and messages.
  - `notifications/` — FCM token management and notification rendering.
  - `ui/screens/` — Compose screens for profile setup, search, and chat.
  - `ui/theme/` — Compose theme definitions.
- `functions/` — Firebase Cloud Functions for sending push notifications.

## Getting started
1. Create a Firebase project with Authentication, Firestore, and Cloud Messaging enabled.
2. Download your `google-services.json` from the Firebase console and place it in `app/google-services.json`.
3. Enable anonymous authentication in Firebase Authentication.
4. Deploy the Cloud Function that sends notifications:
   ```bash
   cd functions
   npm install
   npm run deploy
   ```
5. Build and run the Android app (Android Studio or `./gradlew assembleDebug`). The app signs users in anonymously and prompts for a username and display name.
6. Open the app on two devices or emulators. Search by username to open a chat and exchange messages. The receiving user will get an FCM push notification for new messages.

## Notes
- The Gradle wrapper configuration is included. If the wrapper JAR is missing in your environment, regenerate it with `./gradlew wrapper --gradle-version 8.7` (requires Gradle installed) or download `gradle-wrapper.jar` from a trusted source.
- Remember to request the `POST_NOTIFICATIONS` runtime permission on Android 13+ if you want to see notifications while the app is in the background.
