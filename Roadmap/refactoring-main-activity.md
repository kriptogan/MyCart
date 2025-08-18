# MainActivity Refactoring Roadmap

## Overview
The current `MainActivity.kt` file is **4655 lines** long, which violates the user rule of keeping files small (max ~300 lines). This roadmap outlines the step-by-step process to break it down into smaller, manageable components.

## Project Rules Compliance
- ✅ Keep functions small (4-5 lines of code)
- ✅ Use small files (max ~300 lines)
- ✅ Separate code into directories and connect files if necessary
- ✅ Execute tasks in small steps
- ✅ Always confirm the plan before executing

## Target Architecture
```
app/src/main/java/com/kriptogan/supercart/
├── MainActivity.kt (target: ~100-150 lines)
├── ui/
│   ├── SuperCartApp.kt
│   ├── screens/
│   │   ├── HomeScreen.kt
│   │   └── ShoppingListScreen.kt
│   └── components/
│       ├── CategoryDialogs.kt
│       ├── GroupDialogs.kt
│       ├── ItemDialogs.kt
│       └── UtilityDialogs.kt
├── data/
│   ├── DataStoreExtensions.kt
│   └── MigrationUtils.kt
└── utils/
    ├── StringResources.kt
    └── HelperFunctions.kt
```

## Phase 1: Extract String Resources (Immediate Impact)
**Target:** Reduce MainActivity.kt by ~400 lines
**Priority:** High
**Risk:** Low

### Checklist:
- [ ] Create `app/src/main/java/com/kriptogan/supercart/utils/StringResources.kt`
- [ ] Move `StringResources` object to new file
- [ ] Move `localizedString`, `localizedCategoryName`, `getOriginalCategoryName` functions
- [ ] Update imports in MainActivity.kt
- [ ] Test that localization still works
- [ ] Verify MainActivity.kt line count reduction

**Estimated Time:** 15-20 minutes
**Current Status:** Not Started

---

## Phase 2: Extract Main UI Components (High Impact)
**Target:** Reduce MainActivity.kt by ~2000+ lines
**Priority:** High
**Risk:** Medium

### 2.1 Extract SuperCartApp
**Checklist:**
- [ ] Create `app/src/main/java/com/kriptogan/supercart/ui/SuperCartApp.kt`
- [ ] Move `SuperCartApp` composable function
- [ ] Move `TabItem` data class
- [ ] Update imports and dependencies
- [ ] Test navigation between tabs
- [ ] Verify MainActivity.kt line count reduction

**Estimated Time:** 30-45 minutes
**Current Status:** Not Started

### 2.2 Extract HomeScreen
**Checklist:**
- [ ] Create `app/src/main/java/com/kriptogan/supercart/ui/screens/HomeScreen.kt`
- [ ] Move `HomeScreen` composable function
- [ ] Move all HomeScreen state variables
- [ ] Move HomeScreen helper functions
- [ ] Update imports and dependencies
- [ ] Test HomeScreen functionality
- [ ] Verify MainActivity.kt line count reduction

**Estimated Time:** 45-60 minutes
**Current Status:** Not Started

### 2.3 Extract ShoppingListScreen
**Checklist:**
- [ ] Create `app/src/main/java/com/kriptogan/supercart/ui/screens/ShoppingListScreen.kt`
- [ ] Move `ShoppingListScreen` composable function
- [ ] Move all ShoppingListScreen state variables
- [ ] Move ShoppingListScreen helper functions
- [ ] Update imports and dependencies
- [ ] Test ShoppingListScreen functionality
- [ ] Verify MainActivity.kt line count reduction

**Estimated Time:** 30-45 minutes
**Current Status:** Not Started

---

## Phase 3: Extract Dialog Components (Medium Impact)
**Target:** Reduce MainActivity.kt by ~1500+ lines
**Priority:** Medium
**Risk:** Medium

### 3.1 Extract Category Management Dialogs
**Checklist:**
- [ ] Create `app/src/main/java/com/kriptogan/supercart/ui/components/CategoryDialogs.kt`
- [ ] Move category list dialog
- [ ] Move edit category dialog
- [ ] Move delete category confirmation dialogs
- [ ] Move create category dialog
- [ ] Update imports and dependencies
- [ ] Test category management functionality
- [ ] Verify MainActivity.kt line count reduction

**Estimated Time:** 30-45 minutes
**Current Status:** Not Started

### 3.2 Extract Group Management Dialogs
**Checklist:**
- [ ] Create `app/src/main/java/com/kriptogan/supercart/ui/components/GroupDialogs.kt`
- [ ] Move main sharing dialog
- [ ] Move create group dialog
- [ ] Move join group dialog
- [ ] Move group success/error dialogs
- [ ] Move leave group dialogs
- [ ] Update imports and dependencies
- [ ] Test group management functionality
- [ ] Verify MainActivity.kt line count reduction

**Estimated Time:** 45-60 minutes
**Current Status:** Not Started

### 3.3 Extract Item Management Dialogs
**Checklist:**
- [ ] Create `app/src/main/java/com/kriptogan/supercart/ui/components/ItemDialogs.kt`
- [ ] Move add/edit item dialog
- [ ] Move delete confirmation dialog
- [ ] Move buy history dialog
- [ ] Move duplicate item alert
- [ ] Move import confirmation dialogs
- [ ] Update imports and dependencies
- [ ] Test item management functionality
- [ ] Verify MainActivity.kt line count reduction

**Estimated Time:** 30-45 minutes
**Current Status:** Not Started

### 3.4 Extract Utility Dialogs
**Checklist:**
- [ ] Create `app/src/main/java/com/kriptogan/supercart/ui/components/UtilityDialogs.kt`
- [ ] Move notes dialog (import shopping list)
- [ ] Move language selection dialog
- [ ] Move version dialog
- [ ] Move alert notification dialog
- [ ] Move c-test window (category selection)
- [ ] Update imports and dependencies
- [ ] Test utility dialogs functionality
- [ ] Verify MainActivity.kt line count reduction

**Estimated Time:** 30-45 minutes
**Current Status:** Not Started

---

## Phase 4: Extract Data Management (Medium Impact)
**Target:** Reduce MainActivity.kt by ~300+ lines
**Priority:** Medium
**Risk:** Low

### 4.1 Extract DataStore Extensions
**Checklist:**
- [ ] Create `app/src/main/java/com/kriptogan/supercart/data/DataStoreExtensions.kt`
- [ ] Move all DataStore declarations
- [ ] Move DataStore serializers
- [ ] Update imports in MainActivity.kt
- [ ] Test data persistence
- [ ] Verify MainActivity.kt line count reduction

**Estimated Time:** 20-30 minutes
**Current Status:** Not Started

### 4.2 Extract Migration Functions
**Checklist:**
- [ ] Create `app/src/main/java/com/kriptogan/supercart/data/MigrationUtils.kt`
- [ ] Move `initializeDefaultCustomCategories` function
- [ ] Move `migrateGroceriesToCustomCategories` function
- [ ] Update imports in MainActivity.kt
- [ ] Test migration functionality
- [ ] Verify MainActivity.kt line count reduction

**Estimated Time:** 15-20 minutes
**Current Status:** Not Started

---

## Phase 5: Extract Helper Functions (Low Impact)
**Target:** Reduce MainActivity.kt by ~200+ lines
**Priority:** Low
**Risk:** Low

### 5.1 Extract Helper Functions
**Checklist:**
- [ ] Create `app/src/main/java/com/kriptogan/supercart/utils/HelperFunctions.kt`
- [ ] Move utility functions (if any)
- [ ] Move helper calculations
- [ ] Update imports in MainActivity.kt
- [ ] Test helper functionality
- [ ] Verify MainActivity.kt line count reduction

**Estimated Time:** 15-20 minutes
**Current Status:** Not Started

---

## Phase 6: Clean Up MainActivity (Final Impact)
**Target:** MainActivity.kt should be ~100-150 lines
**Priority:** High
**Risk:** Low

### 6.1 Final Cleanup
**Checklist:**
- [ ] Remove all extracted code from MainActivity.kt
- [ ] Update imports to use new file locations
- [ ] Ensure MainActivity.kt only contains:
  - MainActivity class
  - onCreate method
  - Essential imports
- [ ] Verify MainActivity.kt line count is under 300
- [ ] Test entire app functionality
- [ ] Run compilation to ensure no errors

**Estimated Time:** 30-45 minutes
**Current Status:** Not Started

---

## Testing Strategy
After each phase, we must:
1. **Compile the project** to ensure no syntax errors
2. **Test the specific functionality** that was extracted
3. **Verify line count reduction** in MainActivity.kt
4. **Check that imports are working** correctly

## Risk Mitigation
- **Backup Strategy:** Keep original MainActivity.kt until refactoring is complete
- **Incremental Testing:** Test after each phase, not just at the end
- **Rollback Plan:** If issues arise, we can revert to previous working state

## Success Criteria
- [ ] MainActivity.kt is under 300 lines
- [ ] All functionality works exactly as before
- [ ] Code is organized into logical, maintainable components
- [ ] Each file follows the 300-line rule
- [ ] Project compiles without errors
- [ ] All tests pass (if any exist)

## Estimated Total Time
- **Phase 1:** 15-20 minutes
- **Phase 2:** 105-150 minutes
- **Phase 3:** 135-195 minutes
- **Phase 4:** 35-50 minutes
- **Phase 5:** 15-20 minutes
- **Phase 6:** 30-45 minutes

**Total Estimated Time:** 335-480 minutes (5.5-8 hours)

## Notes
- This refactoring should be done in small, manageable steps
- Each phase should be completed and tested before moving to the next
- User confirmation should be obtained before proceeding to each new phase
- The goal is to maintain exact functionality while improving code organization

---

## Progress Tracking
**Overall Progress:** 0% Complete
**Current Phase:** Not Started
**Next Action:** Awaiting user confirmation to begin Phase 1

**Last Updated:** [Date will be updated as progress is made]
**Updated By:** [Will be updated as progress is made]
