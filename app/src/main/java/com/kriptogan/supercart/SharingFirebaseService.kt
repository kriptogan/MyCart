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
                val group = result.documents[0].toObject(Group::class.java)
                Log.d(TAG, "Group found: ${group?.groupId}")
                group
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
            
            // Update the group document with the new member
            val groupRef = db.collection(GROUPS_COLLECTION).document(groupId)
            
            db.runTransaction { transaction ->
                val groupDoc = transaction.get(groupRef)
                if (groupDoc.exists()) {
                    val group = groupDoc.toObject(Group::class.java)
                    if (group != null) {
                        val updatedMembers = group.members + member
                        val updatedGroup = group.copy(members = updatedMembers)
                        transaction.set(groupRef, updatedGroup)
                    }
                }
            }.await()
            
            Log.d(TAG, "Member added successfully to group: $groupId")
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
            
            // Update the group document by removing the member
            val groupRef = db.collection(GROUPS_COLLECTION).document(groupId)
            
            db.runTransaction { transaction ->
                val groupDoc = transaction.get(groupRef)
                if (groupDoc.exists()) {
                    val group = groupDoc.toObject(Group::class.java)
                    if (group != null) {
                        val updatedMembers = group.members.filter { it.deviceId != deviceId }
                        val updatedGroup = group.copy(members = updatedMembers)
                        transaction.set(groupRef, updatedGroup)
                    }
                }
            }.await()
            
            Log.d(TAG, "Member removed successfully from group: $groupId")
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
            val groups = result.documents.mapNotNull { it.toObject(Group::class.java) }
            
            Log.d(TAG, "Found ${groups.size} groups for device: $deviceId")
            groups
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get groups for device: ${e.message}", e)
            emptyList()
        }
    }
}
