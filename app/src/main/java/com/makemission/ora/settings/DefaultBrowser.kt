package com.makemission.ora.settings

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent

/** Checks and requests the system's default browser role (minSdk 33, so RoleManager is always there). */
object DefaultBrowser {
    fun isDefault(context: Context): Boolean {
        val roles = context.getSystemService(RoleManager::class.java) ?: return false
        return roles.isRoleHeld(RoleManager.ROLE_BROWSER)
    }

    /** The system dialog that asks the user to make Ora the default browser. */
    fun requestIntent(context: Context): Intent? {
        val roles = context.getSystemService(RoleManager::class.java) ?: return null
        return roles.createRequestRoleIntent(RoleManager.ROLE_BROWSER)
    }
}
