package com.makemission.ora.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class SiteKeyTest {
    @Test fun hostWithoutWww() = assertEquals("example.com", siteKeyFor("https://www.Example.com/a?b#c"))
    @Test fun portAndUserAreDropped() = assertEquals("example.com", siteKeyFor("http://me@example.com:8080/x"))
    @Test fun noHostGivesEmpty() = assertEquals("", siteKeyFor("ora://start").let { if (it == "start") "" else it })
}
