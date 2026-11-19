package com.makemission.ora.profiles

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProfileRulesTest {
    private val rules = mapOf("example.com" to "work")

    @Test fun matchesTheSiteAndItsSubdomains() {
        assertEquals("work", ProfileRules.match(rules, "https://www.example.com/a?b=1"))
        assertEquals("work", ProfileRules.match(rules, "https://mail.example.com"))
    }

    @Test fun otherSitesDoNotMatch() {
        assertNull(ProfileRules.match(rules, "https://notexample.com"))
        assertNull(ProfileRules.match(rules, "https://example.org"))
    }

    @Test fun hostOfRejectsNonSites() {
        assertEquals("example.com", ProfileRules.hostOf(" HTTPS://www.Example.com:8080/x "))
        assertNull(ProfileRules.hostOf("two words"))
        assertNull(ProfileRules.hostOf("localhost"))
    }
}
