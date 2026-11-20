package com.makemission.ora.labs

import org.json.JSONObject

/** What the page's own content told Ora (see assets/sitescan). Plain facts, no verdicts. */
data class PageScan(
    val url: String,
    val title: String,
    val h1: String,
    val text: String,
    val words: Int,
    val password: Boolean,
    val card: Boolean,
    val cart: Boolean,
    val linkText: String,
    val generator: String,
    val scripts: String,
) {
    companion object {
        fun from(json: JSONObject) = PageScan(
            json.optString("url"), json.optString("title"), json.optString("h1"), json.optString("text"),
            json.optInt("words"), json.optBoolean("password"), json.optBoolean("card"), json.optBoolean("cart"),
            json.optString("linkText"), json.optString("generator"), json.optString("scripts"),
        )
    }
}

/**
 * Looks for common patterns of scam pages and of pages made by site builders or text generators. Every
 * rule is a heuristic: it lists what it saw and why it matters, and none of it is proof. A page needs
 * several weak signs, or one strong one, before it is mentioned.
 */
object ScamPatterns {
    private val BRANDS = listOf(
        "paypal", "apple", "microsoft", "google", "amazon", "netflix", "facebook", "instagram", "whatsapp", "binance",
        "coinbase", "metamask", "dhl", "fedex", "usps", "chase", "wells fargo", "hsbc", "revolut", "steam",
    )
    private val URGENCY = listOf(
        "limited time", "act now", "hurry", "last chance", "only today", "offer ends", "expires in", "don't miss out",
        "while stocks last", "selling fast", "ends soon", "order within", "immediately or",
    )
    private val SCARCITY = Regex("only \\d{1,2} (left|remaining|in stock)|\\d{1,2} (people|persons|others) (are )?(viewing|watching|bought)")
    private val DEEP_DISCOUNT = Regex("\\b([7-9]\\d)\\s?% off|save up to ([7-9]\\d)%|up to ([7-9]\\d)% off")
    private val PRIZE = listOf("you have won", "you've won", "you are the winner", "claim your prize", "claim your reward", "free gift", "congratulations! you", "lucky visitor")
    private val WALLET = listOf("seed phrase", "recovery phrase", "secret phrase", "private key", "connect your wallet", "validate your wallet", "wallet connect", "12-word", "24-word")
    private val ODD_PAYMENT = listOf("gift card", "western union", "moneygram", "wire transfer only", "bitcoin only", "crypto only", "zelle only", "pay with crypto")
    private val BUILDER_HOSTS = listOf(".lovable.app", ".lovable.dev", ".vercel.app", ".netlify.app", ".framer.website", ".framer.app", ".webflow.io", ".wixsite.com", ".durable.co", ".godaddysites.com", ".weebly.com", ".carrd.co", ".pages.dev", ".github.io", ".web.app", ".replit.app", ".bolt.host")
    private val BUILDER_GENERATORS = listOf("wix", "framer", "webflow", "lovable", "durable", "10web", "hostinger", "squarespace", "bolt", "v0", "weebly", "godaddy", "zyro")
    private val BUILDER_SCRIPTS = listOf("lovable", "gptengineer", "framerusercontent", "webflow.com", "wixstatic", "durablecdn")
    private val RISKY_TLDS = listOf(".top", ".xyz", ".click", ".icu", ".sbs", ".cfd", ".buzz", ".cyou", ".rest", ".monster", ".tk", ".ml", ".cf", ".gq", ".ga")
    private val GENERATED_COPY = listOf(
        "in today's fast-paced", "unlock the power", "elevate your", "seamless experience", "cutting-edge", "game-changer", "game changer",
        "revolutionize", "tailored to your needs", "take your .* to the next level", "state-of-the-art", "delve into", "tapestry of",
        "as an ai language model", "lorem ipsum", "\\[your company", "\\{\\{", "your brand name", "placeholder text",
    ).map { Regex(it) }

    fun inspect(scan: PageScan): List<SiteFinding> {
        val host = scan.url.substringAfter("://", "").substringBefore('/').substringBefore('?').substringBefore(':').lowercase()
        if (host.isBlank()) return emptyList()
        val text = scan.text
        val out = mutableListOf<SiteFinding>()
        var score = 0

        // Strong: asking for a wallet recovery phrase. No real site needs it.
        WALLET.filter { it in text }.takeIf { it.isNotEmpty() }?.let {
            score += 3
            out += SiteFinding("scan.wallet", "Asks about wallet phrases or keys", "Mentions \"${it.first()}\". Real services never need your recovery phrase or private key.", leave = true)
        }

        // Strong: page talks about a well-known brand, has a sign-in, and the address is not that brand's.
        val brand = BRANDS.firstOrNull { b ->
            val bare = b.replace(" ", "")
            (b in scan.title.lowercase() || b in scan.h1.lowercase()) && bare !in host.replace("-", "").replace(".", "")
        }
        if (brand != null && (scan.password || scan.card)) {
            score += 3
            out += SiteFinding("scan.brand", "Sign-in for ${brand.replaceFirstChar { it.uppercase() }} on another address", "This page is about $brand and asks for a password or card, but its address ($host) is not theirs. Check the address before typing anything.", leave = true)
        }

        PRIZE.filter { it in text }.takeIf { it.isNotEmpty() }?.let {
            score += 2
            out += SiteFinding("scan.prize", "Prize or free gift wording", "Says things like \"${it.first()}\". Unexpected prizes are a common lure.")
        }

        val urgent = URGENCY.count { it in text } + if (SCARCITY.containsMatchIn(text)) 1 else 0
        if (urgent >= 2) {
            score += 1
            out += SiteFinding("scan.urgency", "Pressure to act quickly", "Uses countdown, scarcity or \"last chance\" wording ($urgent signs). Pressure is used to stop you thinking.")
        }

        if (DEEP_DISCOUNT.containsMatchIn(text) && (scan.cart || scan.card)) {
            score += 1
            out += SiteFinding("scan.discount", "Very deep discounts", "Shows 70% or more off while selling. Check reviews and the seller before paying.")
        }

        ODD_PAYMENT.filter { it in text }.takeIf { it.isNotEmpty() }?.let {
            score += 2
            out += SiteFinding("scan.payment", "Unusual way to pay", "Mentions \"${it.first()}\". Payments you cannot reverse are favoured by scammers.")
        }

        if ((scan.cart || scan.card) && listOf("contact", "about", "privacy", "terms", "refund", "return").count { it in scan.linkText } < 2) {
            score += 1
            out += SiteFinding("scan.nolegal", "A shop without the usual pages", "Sells things but has few links to contact, returns, privacy or terms pages.")
        }

        val risky = RISKY_TLDS.any { host.endsWith(it) }
        val hyphens = host.count { it == '-' }
        val brandInSub = BRANDS.any { b -> val bare = b.replace(" ", ""); bare in host.substringBeforeLast('.').substringBeforeLast('.') && !host.endsWith("$bare.com") }
        if (brandInSub || (risky && hyphens >= 1) || hyphens >= 3) {
            score += if (brandInSub) 2 else 1
            out += SiteFinding("scan.domain", "Address looks unusual", if (brandInSub) "A brand name appears in the address but is not the real site's own domain." else "Long or hyphenated address, or a type of ending often used for throwaway sites.")
        }

        // Site-builder and generated-page signs. A builder alone is not a problem; it is noted, and it only counts with other signs.
        val builtBy = when {
            BUILDER_HOSTS.any { host.endsWith(it) } -> host.substringAfter('.')
            BUILDER_GENERATORS.any { it in scan.generator.lowercase() } -> scan.generator
            BUILDER_SCRIPTS.any { it in scan.scripts } -> BUILDER_SCRIPTS.first { it in scan.scripts }
            else -> ""
        }
        val generated = GENERATED_COPY.count { it.containsMatchIn(text) }
        if (builtBy.isNotEmpty() || generated >= 3) {
            val why = buildList {
                if (builtBy.isNotEmpty()) add("made or hosted with a site builder ($builtBy)")
                if (generated >= 3) add("reads like stock or generated marketing copy ($generated typical phrases)")
            }.joinToString(" and ")
            if (score >= 1) score += 1
            out += SiteFinding("scan.builder", "Looks quickly built", "This page appears to be $why. Many honest sites are too, so this only matters with other warnings.")
        }

        // Quiet unless it adds up: a lone weak sign is dropped, so ordinary shops and blogs stay unbothered.
        val signs = out.filter { it.id != "scan.builder" }
        if (score < 2 && signs.none { it.leave }) return out.filter { it.id == "scan.builder" && generated >= 3 && builtBy.isNotEmpty() }
        return out
    }
}
