# SuperCart Cloud Functions

This directory contains the Cloud Functions for the SuperCart app's push notification system.

## Functions

### 1. `checkExpiredItems`
- **Purpose**: Checks for expired, expiring, and due items in a family project
- **Trigger**: Called manually from the Android app
- **Actions**: 
  - Identifies expired items (past expiration date)
  - Identifies expiring items (within 3 days)
  - Identifies items due for purchase (based on average buying cycle)
  - Sends notifications to family members based on their preferences

### 2. `notifyShoppingListItemsAdded`
- **Purpose**: Notifies family members when items are added to shopping list
- **Trigger**: Called when user clicks the bell icon in shopping list
- **Actions**: Sends notifications to family members who have enabled item notifications

## Deployment

### Prerequisites
1. Install Firebase CLI: `npm install -g firebase-tools`
2. Login to Firebase: `firebase login`
3. Select your project: `firebase use <your-project-id>`

### Deploy Functions
```bash
cd functions
npm install
firebase deploy --only functions
```

### Test Functions
```bash
# Test locally (requires Firebase emulator)
firebase emulators:start --only functions

# View logs
firebase functions:log
```

## Configuration

The functions automatically use your Firebase project configuration. No additional setup required.

## Cost
- **Cloud Functions**: Free tier includes 125K invocations/month
- **FCM**: Free for unlimited notifications
- **Firestore**: Free tier includes 50K reads/day

## Manual vs Automatic
- **Current**: Functions run manually when triggered from the app
- **Future**: Can be scheduled to run automatically using Cloud Scheduler ($0.10/month)
