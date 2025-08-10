const functions = require('firebase-functions');
const admin = require('firebase-admin');

admin.initializeApp();

const db = admin.firestore();

// Function to check for expired items and send notifications
exports.checkExpiredItems = functions.https.onCall(async (data, context) => {
    try {
        const { projectId } = data;
        
        if (!projectId) {
            throw new Error('Project ID is required');
        }

        // Get the family project
        const projectDoc = await db.collection('family_projects').doc(projectId).get();
        if (!projectDoc.exists) {
            throw new Error('Project not found');
        }

        const project = projectDoc.data();
        const groceries = project.groceries || [];
        const currentDate = new Date();
        
        // Check for expired and expiring items
        const expiredItems = [];
        const expiringItems = [];
        const dueItems = [];

        groceries.forEach(grocery => {
            // Check expiration
            if (grocery.expirationDate) {
                const expirationDate = grocery.expirationDate.toDate();
                const daysUntilExpiry = Math.ceil((expirationDate - currentDate) / (1000 * 60 * 60 * 24));
                
                if (daysUntilExpiry < 0) {
                    expiredItems.push(grocery.name);
                } else if (daysUntilExpiry <= 3) {
                    expiringItems.push(grocery.name);
                }
            }

            // Check average buying due
            if (grocery.averageBuyingDays && grocery.lastTimeBoughtDays) {
                const daysSinceLastBought = grocery.lastTimeBoughtDays;
                if (daysSinceLastBought >= grocery.averageBuyingDays) {
                    dueItems.push(grocery.name);
                }
            }
        });

        // Get project members and their notification settings
        const deviceRegistrations = await db.collection('device_registrations')
            .where('projectId', '==', projectId)
            .get();

        const notifications = [];

        deviceRegistrations.forEach(doc => {
            const registration = doc.data();
            const settings = registration.notificationSettings || {};
            const fcmToken = registration.fcmToken;

            if (!fcmToken) return;

            // Send expiration notifications
            if (settings.notifyExpiration && (expiredItems.length > 0 || expiringItems.length > 0)) {
                if (expiredItems.length > 0 || expiringItems.length > 0) {
                    const title = 'SuperCart - Item Expiration Alert';
                    let body = '';
                    
                    if (expiredItems.length > 0) {
                        body += `Expired: ${expiredItems.slice(0, 3).join(', ')}`;
                        if (expiredItems.length > 3) body += ` and ${expiredItems.length - 3} more`;
                    }
                    
                    if (expiringItems.length > 0) {
                        if (body) body += '\n';
                        body += `Expiring soon: ${expiringItems.slice(0, 3).join(', ')}`;
                        if (expiringItems.length > 3) body += ` and ${expiringItems.length - 3} more`;
                    }

                    notifications.push({
                        token: fcmToken,
                        notification: { title, body },
                        data: {
                            type: 'expiration',
                            projectId: projectId,
                            channelId: 'expiration'
                        }
                    });
                }
            }

            // Send average buying due notifications
            if (settings.notifyAverageDue && dueItems.length > 0) {
                const title = 'SuperCart - Items Due for Purchase';
                const body = `Time to buy: ${dueItems.slice(0, 5).join(', ')}${dueItems.length > 5 ? ` and ${dueItems.length - 5} more` : ''}`;

                notifications.push({
                    token: fcmToken,
                    notification: { title, body },
                    data: {
                        type: 'average_due',
                        projectId: projectId,
                        channelId: 'expiration'
                    }
                });
            }
        });

        // Send all notifications
        if (notifications.length > 0) {
            const batch = notifications.map(notification => 
                admin.messaging().send(notification)
            );
            
            await Promise.all(batch);
            console.log(`Sent ${notifications.length} notifications for project ${projectId}`);
        }

        return {
            success: true,
            expiredItems: expiredItems.length,
            expiringItems: expiringItems.length,
            dueItems: dueItems.length,
            notificationsSent: notifications.length
        };

    } catch (error) {
        console.error('Error checking expired items:', error);
        throw new functions.https.HttpsError('internal', error.message);
    }
});

// Function to notify family members about shopping list items added
exports.notifyShoppingListItemsAdded = functions.https.onCall(async (data, context) => {
    try {
        const { projectId, items, senderDeviceId } = data;
        
        if (!projectId || !items || items.length === 0) {
            throw new Error('Project ID and items are required');
        }

        // Get project members and their notification settings
        const deviceRegistrations = await db.collection('device_registrations')
            .where('projectId', '==', projectId)
            .get();

        const notifications = [];

        deviceRegistrations.forEach(doc => {
            const registration = doc.data();
            const settings = registration.notificationSettings || {};
            const fcmToken = registration.fcmToken;

            // Skip sender and devices without FCM tokens
            if (!fcmToken || registration.deviceId === senderDeviceId) return;

            // Only send if user wants item added notifications
            if (settings.notifyItemsAdded) {
                const title = 'SuperCart - New Items Added';
                const body = `New items added to shopping list: ${items.slice(0, 3).join(', ')}${items.length > 3 ? ` and ${items.length - 3} more` : ''}`;

                notifications.push({
                    token: fcmToken,
                    notification: { title, body },
                    data: {
                        type: 'items_added',
                        projectId: projectId,
                        items: JSON.stringify(items),
                        channelId: 'shopping_list'
                    }
                });
            }
        });

        // Send all notifications
        if (notifications.length > 0) {
            const batch = notifications.map(notification => 
                admin.messaging().send(notification)
            );
            
            await Promise.all(batch);
            console.log(`Sent ${notifications.length} shopping list notifications for project ${projectId}`);
        }

        return {
            success: true,
            notificationsSent: notifications.length
        };

    } catch (error) {
        console.error('Error notifying shopping list items:', error);
        throw new functions.https.HttpsError('internal', error.message);
    }
});
