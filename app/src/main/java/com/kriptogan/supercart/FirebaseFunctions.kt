package com.kriptogan.supercart

import com.google.firebase.functions.FirebaseFunctions

object FirebaseFunctionsHelper {
    private val functions: FirebaseFunctions by lazy { FirebaseFunctions.getInstance() }

    fun triggerShoppingListNotification(projectId: String, itemNames: List<String>) {
        val data = hashMapOf(
            "projectId" to projectId,
            "items" to itemNames
        )
        functions
            .getHttpsCallable("notifyShoppingListItemsAdded")
            .call(data)
    }
}


