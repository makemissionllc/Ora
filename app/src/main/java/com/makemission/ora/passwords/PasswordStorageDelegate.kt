package com.makemission.ora.passwords

import com.makemission.ora.log.BrowserLog
import com.makemission.ora.log.UsageStats
import org.mozilla.geckoview.Autocomplete
import org.mozilla.geckoview.GeckoResult

/**
 * Connects GeckoView's login autofill to [PasswordStore]. GeckoView fills forms from the
 * logins returned here and hands back the logins a user submits.
 */
object PasswordStorageDelegate : Autocomplete.StorageDelegate {

    override fun onLoginFetch(domain: String): GeckoResult<Array<Autocomplete.LoginEntry>> {
        val matches = PasswordStore.forHost(domain).map { it.toEntry() }.toTypedArray()
        return GeckoResult.fromValue(matches)
    }

    override fun onLoginFetch(): GeckoResult<Array<Autocomplete.LoginEntry>> =
        GeckoResult.fromValue(PasswordStore.logins.value.map { it.toEntry() }.toTypedArray())

    override fun onLoginSave(entry: Autocomplete.LoginEntry) {
        try {
            PasswordStore.save(entry.origin, entry.username.orEmpty(), entry.password.orEmpty())
        } catch (e: Exception) {
            BrowserLog.error("passwords", "Login from a page could not be saved", e)
        }
    }

    override fun onLoginUsed(entry: Autocomplete.LoginEntry, type: Int) {
        BrowserLog.info("passwords", "Filled a saved login")
        UsageStats.count("passwords.autofill")
    }

    private fun PasswordStore.Login.toEntry(): Autocomplete.LoginEntry =
        Autocomplete.LoginEntry.Builder()
            .guid(guid)
            .origin(origin)
            .formActionOrigin(origin)
            .username(username)
            .password(password)
            .build()
}
