# Firebase Sharing Feature Implementation Plan

## Overview
Implement a sharing mechanism using Firebase that allows users to create or join groups to share grocery lists and categories.

## Requirements Summary
- Users can create groups with unique 8-digit codes
- Users can join existing groups using codes
- Data scope: groceries + categories only
- No real-time sync (manual sync only)
- No conflict resolution (separate task)
- Users can leave groups
- One group per user at a time
- Local data when not in group, Local+Firebase when in group

## Implementation Steps

### Phase 1: Foundation & Data Models
- [x] **Step 1.1**: Create sharing group data models (Group, GroupMember, etc.)
- [x] **Step 1.2**: Set up Firestore collections structure
- [x] **Step 1.3**: Implement 8-digit code generation logic
- [x] **Step 1.4**: Add group state management to MainActivity

### Phase 2: UI Components
- [x] **Step 2.1**: Add "Sharing group" option to burger menu
- [x] **Step 2.2**: Create main sharing dialog with Create/Join options
- [x] **Step 2.3**: Implement "Create Group" dialog
- [x] **Step 2.4**: Implement "Join Group" dialog with code input
- [x] **Step 2.5**: Add group status display (current group info)

### Phase 3: Firebase Operations
- [x] **Step 3.1**: Implement group creation in Firestore
- [x] **Step 3.2**: Implement group joining logic
- [x] **Step 3.3**: Add data upload functionality (groceries + categories)
- [x] **Step 3.4**: Add data download functionality
- [x] **Step 3.5**: Implement leave group functionality

### Phase 4: Data Synchronization
- [ ] **Step 4.1**: Implement data overwrite logic when joining group
- [ ] **Step 4.2**: Add user confirmation for data overwrite
- [ ] **Step 4.3**: Handle group state persistence
- [ ] **Step 4.4**: Add error handling and validation

### Phase 5: Testing & Polish
- [ ] **Step 5.1**: Test group creation flow
- [ ] **Step 5.2**: Test group joining flow
- [ ] **Step 5.3**: Test data upload/download
- [ ] **Step 5.4**: Test leave group functionality
- [ ] **Step 5.5**: UI/UX improvements and edge case handling

## Data Models to Create

### Group
```kotlin
data class Group(
    val groupId: String,
    val groupCode: String, // 8-digit code
    val ownerId: String,
    val members: List<GroupMember>,
    val createdAt: String,
    val lastSyncAt: String
)
```

### GroupMember
```kotlin
data class GroupMember(
    val userId: String,
    val deviceId: String,
    val joinedAt: String,
    val lastActiveAt: String
)
```

### GroupData
```kotlin
data class GroupData(
    val groupId: String,
    val groceries: List<Grocery>,
    val categories: List<CustomCategory>,
    val lastUpdatedAt: String
)
```

## Firestore Collections Structure

### groups
- Document ID: auto-generated
- Fields: Group data

### group_data
- Document ID: groupId
- Fields: GroupData (groceries + categories)

### group_members
- Document ID: groupId
- Fields: List of GroupMember objects

## Current Status
- [x] Firebase connection established and tested
- [ ] Ready to start implementation

## Next Action
Begin with **Step 1.1**: Create sharing group data models

## Notes
- Keep implementation incremental
- Test each step before moving to next
- Maintain existing app functionality
- Follow existing code patterns and style
