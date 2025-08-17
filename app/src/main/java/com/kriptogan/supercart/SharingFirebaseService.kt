package com.kriptogan.supercart

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import android.util.Log

/**
 * Firebase service for sharing operations
 * Handles groups, group data, and group members collections
 */
class SharingFirebaseService {
    private val db = FirebaseFirestore.getInstance()
    
    companion object {
        private const val TAG = "SharingFirebaseService"
        
        // Collection names as defined in the implementation plan
        private const val GROUPS_COLLECTION = "groups"
        private const val GROUP_DATA_COLLECTION = "group_data"
        private const val GROUP_MEMBERS_COLLECTION = "group_members"
        
        // Field names for queries
        private const val FIELD_GROUP_CODE = "groupCode"
        private const val FIELD_OWNER_ID = "ownerId"
        private const val FIELD_GROUP_ID = "groupId"
        private const val FIELD_DEVICE_ID = "deviceId"
    }
    
    /**
     * Create a new group in Firestore
     * @param group The group to create
     * @return The created group ID, or null if failed
     */
    suspend fun createGroup(group: Group): String? {
        return try {
            Log.d(TAG, "Creating group with code: ${group.groupCode}")
            
            // Add to groups collection
            val groupDoc = db.collection(GROUPS_COLLECTION).add(group).await()
            val groupId = groupDoc.id
            
            Log.d(TAG, "Group created successfully with ID: $groupId")
            
            // Update the group object with the actual Firestore document ID
            val updatedGroup = group.copy(groupId = groupId)
            
            // Update the document with the correct groupId
            db.collection(GROUPS_COLLECTION)
                .document(groupId)
                .set(updatedGroup)
                .await()
            
            Log.d(TAG, "Group document updated with correct groupId: $groupId")
            
            // Create initial group data document
            val initialGroupData = GroupData(
                groupId = groupId,
                groceries = emptyList(),
                categories = emptyList(),
                lastModifiedBy = group.ownerId
            )
            
            db.collection(GROUP_DATA_COLLECTION)
                .document(groupId)
                .set(initialGroupData)
                .await()
            
            Log.d(TAG, "Initial group data created for group: $groupId")
            
            groupId
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create group: ${e.message}", e)
            null
        }
    }
    
    /**
     * Find a group by its 8-digit code
     * @param groupCode The 8-digit group code
     * @return The group if found, null otherwise
     */
    suspend fun findGroupByCode(groupCode: String): Group? {
        return try {
            Log.d(TAG, "Searching for group with code: $groupCode")
            
            val query = db.collection(GROUPS_COLLECTION)
                .whereEqualTo(FIELD_GROUP_CODE, groupCode)
                .limit(1)
            
            val result = query.get().await()
            
            if (result.isEmpty) {
                Log.d(TAG, "No group found with code: $groupCode")
                null
            } else {
                val document = result.documents[0]
                val actualDocumentId = document.id
                Log.d(TAG, "Group document found with ID: $actualDocumentId")
                
                val group = document.toObject(Group::class.java)
                if (group != null) {
                    Log.d(TAG, "Original group object has groupId: ${group.groupId}")
                    Log.d(TAG, "Firestore document ID: $actualDocumentId")
                    
                    // Create a new group object with the correct Firestore document ID
                    val correctedGroup = group.copy(groupId = actualDocumentId)
                    Log.d(TAG, "Corrected group object has groupId: ${correctedGroup.groupId}")
                    correctedGroup
                } else {
                    Log.e(TAG, "Failed to deserialize group document")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to find group by code: ${e.message}", e)
            null
        }
    }
    
    /**
     * Get group data (groceries and categories) for a specific group
     * @param groupId The group ID
     * @return The group data if found, null otherwise
     */
    suspend fun getGroupData(groupId: String): GroupData? {
        return try {
            Log.d(TAG, "Fetching group data for group: $groupId")
            
            val doc = db.collection(GROUP_DATA_COLLECTION)
                .document(groupId)
                .get()
                .await()
            
            if (doc.exists()) {
                val groupData = doc.toObject(GroupData::class.java)
                Log.d(TAG, "Group data retrieved successfully")
                groupData
            } else {
                Log.d(TAG, "Group data not found for group: $groupId")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get group data: ${e.message}", e)
            null
        }
    }
    
    /**
     * Update group data (groceries and categories) for a specific group
     * @param groupData The updated group data
     * @return True if successful, false otherwise
     */
    suspend fun updateGroupData(groupData: GroupData): Boolean {
        return try {
            Log.d(TAG, "Updating group data for group: ${groupData.groupId}")
            
            db.collection(GROUP_DATA_COLLECTION)
                .document(groupData.groupId)
                .set(groupData)
                .await()
            
            Log.d(TAG, "Group data updated successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update group data: ${e.message}", e)
            false
        }
    }
    
    /**
     * Add a member to a group
     * @param groupId The group ID
     * @param member The member to add
     * @return True if successful, false otherwise
     */
    suspend fun addMemberToGroup(groupId: String, member: GroupMember): Boolean {
        return try {
            Log.d(TAG, "Adding member ${member.deviceId} to group: $groupId")
            Log.d(TAG, "Group ID being used: $groupId")
            
            // First, try to find the group by the provided groupId to get the correct document ID
            var actualGroupId = groupId
            var groupRef = db.collection(GROUPS_COLLECTION).document(groupId)
            
            // Verify document exists before transaction
            var preCheckDoc = groupRef.get().await()
            Log.d(TAG, "Pre-transaction check - Group document exists: ${preCheckDoc.exists()}")
            Log.d(TAG, "Pre-transaction check - Group document ID: ${preCheckDoc.id}")
            Log.d(TAG, "Pre-transaction check - Expected groupId: $groupId")
            Log.d(TAG, "Pre-transaction check - Actual document ID: ${preCheckDoc.id}")
            Log.d(TAG, "Pre-transaction check - IDs match: ${groupId == preCheckDoc.id}")
            
            // If the document doesn't exist with the provided groupId, try to find it by searching
            if (!preCheckDoc.exists()) {
                Log.d(TAG, "Group document not found with provided groupId, searching for group...")
                
                // Try to find the group by searching for the group code or other identifiers
                // For now, we'll return false since we need more context to find the right group
                Log.e(TAG, "Cannot add member: group document not found and no way to locate it")
                return false
            }
            
            val transactionResult = db.runTransaction { transaction ->
                val groupDoc = transaction.get(groupRef)
                Log.d(TAG, "Group document exists: ${groupDoc.exists()}")
                Log.d(TAG, "Group document ID: ${groupDoc.id}")
                Log.d(TAG, "Group document path: ${groupDoc.reference.path}")
                
                if (groupDoc.exists()) {
                    try {
                        val group = groupDoc.toObject(Group::class.java)
                        Log.d(TAG, "Group deserialized successfully: $group")
                        
                        if (group != null) {
                            val updatedMembers = group.members + member
                            Log.d(TAG, "Updated members list: $updatedMembers")
                            
                            val updatedGroup = group.copy(members = updatedMembers)
                            Log.d(TAG, "Updated group: $updatedGroup")
                            
                            transaction.set(groupRef, updatedGroup)
                            Log.d(TAG, "Transaction set operation completed")
                        } else {
                            Log.e(TAG, "Failed to deserialize group document")
                            throw IllegalStateException("Failed to deserialize group document")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error during group deserialization: ${e.message}", e)
                        throw e
                    }
                } else {
                    Log.e(TAG, "Group document does not exist for ID: $groupId")
                    throw IllegalStateException("Group document does not exist for ID: $groupId")
                }
            }
            
            Log.d(TAG, "Transaction completed successfully")
            transactionResult.await()
            
            // Verify the change was actually committed
            val postCheckDoc = groupRef.get().await()
            Log.d(TAG, "Post-transaction check - Group document exists: ${postCheckDoc.exists()}")
            if (postCheckDoc.exists()) {
                val postGroup = postCheckDoc.toObject(Group::class.java)
                Log.d(TAG, "Post-transaction check - Members count: ${postGroup?.members?.size}")
            }
            
            Log.d(TAG, "Member added successfully to group: $actualGroupId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add member to group: ${e.message}", e)
            false
        }
    }
    
    /**
     * Remove a member from a group
     * @param groupId The group ID
     * @param deviceId The device ID of the member to remove
     * @return True if successful, false otherwise
     */
    suspend fun removeMemberFromGroup(groupId: String, deviceId: String): Boolean {
        return try {
            Log.d(TAG, "Removing member $deviceId from group: $groupId")
            
            // First, try to find the group by the provided groupId to get the correct document ID
            var actualGroupId = groupId
            var groupRef = db.collection(GROUPS_COLLECTION).document(groupId)
            
            // Verify document exists before transaction
            var preCheckDoc = groupRef.get().await()
            Log.d(TAG, "Pre-transaction check - Group document exists: ${preCheckDoc.exists()}")
            Log.d(TAG, "Pre-transaction check - Group document ID: ${preCheckDoc.id}")
            
            // If the document doesn't exist with the provided groupId, try to find it by searching
            if (!preCheckDoc.exists()) {
                Log.d(TAG, "Group document not found with provided groupId, searching for group...")
                
                // Try to find the group by searching for the device in any group
                val query = db.collection(GROUPS_COLLECTION)
                    .whereArrayContains("members", mapOf("deviceId" to deviceId))
                    .limit(1)
                
                val result = query.get().await()
                if (!result.isEmpty) {
                    val foundGroup = result.documents[0]
                    actualGroupId = foundGroup.id
                    groupRef = db.collection(GROUPS_COLLECTION).document(actualGroupId)
                    Log.d(TAG, "Found group with actual document ID: $actualGroupId")
                    
                    // Re-check with the correct document ID
                    preCheckDoc = groupRef.get().await()
                    Log.d(TAG, "Re-check - Group document exists: ${preCheckDoc.exists()}")
                    Log.d(TAG, "Re-check - Group document ID: ${preCheckDoc.id}")
                } else {
                    Log.e(TAG, "No group found containing member: $deviceId")
                    return false
                }
            }
            
            val transactionResult = db.runTransaction { transaction ->
                val groupDoc = transaction.get(groupRef)
                Log.d(TAG, "Group document exists: ${groupDoc.exists()}")
                Log.d(TAG, "Group document ID: ${groupDoc.id}")
                Log.d(TAG, "Group document path: ${groupDoc.reference.path}")
                
                if (groupDoc.exists()) {
                    try {
                        val group = groupDoc.toObject(Group::class.java)
                        Log.d(TAG, "Group deserialized successfully: $group")
                        
                        if (group != null) {
                            val updatedMembers = group.members.filter { it.deviceId != deviceId }
                            Log.d(TAG, "Updated members list: $updatedMembers")
                            
                            val updatedGroup = group.copy(members = updatedMembers)
                            Log.d(TAG, "Updated group: $updatedGroup")
                            
                            transaction.set(groupRef, updatedGroup)
                            Log.d(TAG, "Transaction set operation completed")
                        } else {
                            Log.e(TAG, "Failed to deserialize group document")
                            throw IllegalStateException("Failed to deserialize group document")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error during group deserialization: ${e.message}", e)
                        throw e
                    }
                } else {
                    Log.e(TAG, "Group document does not exist for ID: $groupId")
                    throw IllegalStateException("Group document does not exist for ID: $groupId")
                }
            }
            
            Log.d(TAG, "Transaction completed successfully")
            transactionResult.await()
            
            // Verify the change was actually committed
            val postCheckDoc = groupRef.get().await()
            Log.d(TAG, "Post-transaction check - Group document exists: ${postCheckDoc.exists()}")
            if (postCheckDoc.exists()) {
                val postGroup = postCheckDoc.toObject(Group::class.java)
                Log.d(TAG, "Post-transaction check - Members count: ${postGroup?.members?.size}")
            }
            
            Log.d(TAG, "Member removed successfully from group: $actualGroupId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove member from group: ${e.message}", e)
            false
        }
    }
    
    /**
     * Delete a group and all its associated data
     * @param groupId The group ID to delete
     * @return True if successful, false otherwise
     */
    suspend fun deleteGroup(groupId: String): Boolean {
        return try {
            Log.d(TAG, "Deleting group: $groupId")
            
            // Delete group data
            db.collection(GROUP_DATA_COLLECTION)
                .document(groupId)
                .delete()
                .await()
            
            // Delete group members
            db.collection(GROUP_MEMBERS_COLLECTION)
                .document(groupId)
                .delete()
                .await()
            
            // Delete the group itself
            db.collection(GROUPS_COLLECTION)
                .document(groupId)
                .delete()
                .await()
            
            Log.d(TAG, "Group deleted successfully: $groupId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete group: ${e.message}", e)
            false
        }
    }
    
    /**
     * Check if a group code already exists
     * @param groupCode The group code to check
     * @return True if the code exists, false otherwise
     */
    suspend fun isGroupCodeExists(groupCode: String): Boolean {
        return try {
            val query = db.collection(GROUPS_COLLECTION)
                .whereEqualTo(FIELD_GROUP_CODE, groupCode)
                .limit(1)
            
            val result = query.get().await()
            result.size() > 0
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check if group code exists: ${e.message}", e)
            false
        }
    }
    
    /**
     * Refresh group data by fetching the latest version from Firebase
     * @param groupId The group ID to refresh
     * @return The refreshed group data, or null if failed
     */
    suspend fun refreshGroupData(groupId: String): Group? {
        return try {
            Log.d(TAG, "Refreshing group data for group: $groupId")
            getGroupById(groupId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to refresh group data: ${e.message}", e)
            null
        }
    }

    /**
     * Get a group by its ID
     * @param groupId The group ID
     * @return The group if found, null otherwise
     */
    suspend fun getGroupById(groupId: String): Group? {
        return try {
            Log.d(TAG, "Fetching group by ID: $groupId")
            
            val groupDoc = db.collection(GROUPS_COLLECTION)
                .document(groupId)
                .get()
                .await()
            
            if (groupDoc.exists()) {
                val group = groupDoc.toObject(Group::class.java)
                Log.d(TAG, "Group found by ID: $groupId, members count: ${group?.members?.size}")
                group
            } else {
                Log.d(TAG, "Group not found by ID: $groupId")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get group by ID: ${e.message}", e)
            null
        }
    }

    /**
     * Get all groups where a device is a member
     * @param deviceId The device ID to search for
     * @return List of groups where the device is a member
     */
    suspend fun getGroupsForDevice(deviceId: String): List<Group> {
        return try {
            Log.d(TAG, "Fetching groups for device: $deviceId")
            
            val query = db.collection(GROUPS_COLLECTION)
                .whereArrayContains("members", mapOf("deviceId" to deviceId))
            
            val result = query.get().await()
            val groups = result.documents.mapNotNull { doc ->
                val group = doc.toObject(Group::class.java)
                if (group != null) {
                    // Ensure the groupId matches the actual Firestore document ID
                    group.copy(groupId = doc.id)
                } else {
                    null
                }
            }
            
            Log.d(TAG, "Found ${groups.size} groups for device: $deviceId")
            groups
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get groups for device: ${e.message}", e)
            emptyList()
        }
    }
    
    /**
     * Fix existing groups that have incorrect groupId values
     * This method updates all existing groups to have the correct Firestore document ID
     */
    suspend fun fixExistingGroups(): Boolean {
        return try {
            Log.d(TAG, "Starting to fix existing groups...")
            
            val result = db.collection(GROUPS_COLLECTION).get().await()
            var fixedCount = 0
            
            for (doc in result.documents) {
                val group = doc.toObject(Group::class.java)
                if (group != null && group.groupId != doc.id) {
                    Log.d(TAG, "Fixing group: ${group.groupId} -> ${doc.id}")
                    
                    val correctedGroup = group.copy(groupId = doc.id)
                    doc.reference.set(correctedGroup).await()
                    fixedCount++
                }
            }
            
            Log.d(TAG, "Fixed $fixedCount groups")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fix existing groups: ${e.message}", e)
            false
        }
    }
}
