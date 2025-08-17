package com.kriptogan.supercart

import java.time.LocalDateTime
import java.util.UUID
import kotlinx.serialization.Serializable

/**
 * Represents a sharing group that allows users to share grocery lists and categories
 */
@Serializable
data class Group @JvmOverloads constructor(
    val groupId: String = UUID.randomUUID().toString(),
    val groupCode: String = "",
    val ownerId: String = "",
    val members: List<GroupMember> = emptyList(),
    val createdAt: String = LocalDateTime.now().toString(), // ISO string format
    val lastSyncAt: String = LocalDateTime.now().toString() // ISO string format
)

/**
 * Represents a member of a sharing group
 */
@Serializable
data class GroupMember @JvmOverloads constructor(
    val userId: String = "", // Device ID of the member
    val deviceId: String = "", // Device identifier
    val joinedAt: String = LocalDateTime.now().toString(), // ISO string format
    val lastActiveAt: String = LocalDateTime.now().toString() // ISO string format
)

/**
 * Represents the shared data within a group (groceries + categories)
 */
@Serializable
data class GroupData @JvmOverloads constructor(
    val groupId: String = "",
    val groceries: List<Grocery> = emptyList(),
    val categories: List<CustomCategory> = emptyList(),
    val lastUpdatedAt: String = LocalDateTime.now().toString(), // ISO string format
    val lastModifiedBy: String = "" // Device ID of who made the last change
)

/**
 * Represents the current group state for a user
 */
@Serializable
data class GroupState(
    val isInGroup: Boolean = false,
    val currentGroupId: String? = null,
    val currentGroupCode: String? = null,
    val isOwner: Boolean = false,
    val lastSyncAt: String? = null
)

/**
 * Utility functions for sharing models
 */
object SharingUtils {
    
    /**
     * Generate a unique 8-digit group code
     */
    fun generateGroupCode(): String {
        val random = (10000000..99999999).random()
        return random.toString()
    }
    
    /**
     * Validate if a group code is in correct format
     */
    fun isValidGroupCode(code: String): Boolean {
        return code.length == 8 && code.all { it.isDigit() }
    }
    
    /**
     * Create a new group with the current device as owner
     */
    fun createGroup(deviceId: String): Group {
        val groupCode = generateGroupCode()
        val owner = GroupMember(
            userId = deviceId,
            deviceId = deviceId
        )
        
        return Group(
            groupCode = groupCode,
            ownerId = deviceId,
            members = listOf(owner)
        )
    }
    
    /**
     * Add a member to an existing group
     */
    fun addMemberToGroup(group: Group, deviceId: String): Group {
        val newMember = GroupMember(
            userId = deviceId,
            deviceId = deviceId
        )
        
        return group.copy(
            members = group.members + newMember
        )
    }
    
    /**
     * Remove a member from a group
     */
    fun removeMemberFromGroup(group: Group, deviceId: String): Group {
        return group.copy(
            members = group.members.filter { it.deviceId != deviceId }
        )
    }
    
    /**
     * Check if a device is a member of a group
     */
    fun isMemberOfGroup(group: Group, deviceId: String): Boolean {
        return group.members.any { it.deviceId == deviceId }
    }
    
    /**
     * Check if a device is the owner of a group
     */
    fun isOwnerOfGroup(group: Group, deviceId: String): Boolean {
        return group.ownerId == deviceId
    }
}
