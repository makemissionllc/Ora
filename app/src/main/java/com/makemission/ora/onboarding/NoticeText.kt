package com.makemission.ora.onboarding

/** Bump this when the notice text changes: everyone sees the notice again. */
const val NOTICE_VERSION = 1

/** The five short points on the "Before you start" screen (title, then words). */
val NoticeShort: List<Pair<String, String>> = listOf(
    "Ora is experimental" to "Ora is a personal project that's still being built. It hasn't had a security audit.",
    "Keep important things elsewhere" to "Use your main browser for banking, payments, health and work accounts.",
    "Your browsing stays here" to "Your browsing stays on this device. Ora has no servers and collects nothing.",
    "A diagnostic log, on this device" to "A diagnostic log on this device helps find bugs. It keeps site names only, never full addresses, searches or private tabs, and leaves only if you export it.",
)

const val NoticeSmallPrint = "Under Kenya's Data Protection Act, 2019 you can ask what's kept about you and have it deleted. In Ora you can see and delete everything yourself in Settings. Ora is provided as-is, without warranty."

/** The full notice: heading and paragraph, in order. Shown at Settings > About > Notice and privacy. */
val NoticeFull: List<Pair<String, String>> = listOf(
    "Ora is experimental." to "Ora is a personal, educational project in active development. It has not been independently audited for security. Don't use it for banking, payments, health records, work accounts or anything else you can't afford to have exposed.",
    "What stays on your device." to "Tabs, history, bookmarks, saved passwords, downloads, settings and anything Ora learns about how you browse are stored only on this device, separately for each profile. Ora has no servers and does not send this data anywhere. Websites you visit and services you choose (for example your search engine) receive what any browser sends them.",
    "The diagnostic log." to "Ora keeps a log on this device to find and fix bugs. It records events such as \"page loaded on example.com\" and errors. It never records full web addresses, what you search for, or anything from private tabs. The log is kept up to about 5,000 lines. It leaves your device only if you export and share it yourself. You can view and clear it in Settings > Diagnostics.",
    "Your rights." to "If you are in Kenya, the Data Protection Act, 2019 gives you rights over your personal data, including to be told how it is used, to access it, to correct it and to have it deleted. The Office of the Data Protection Commissioner (odpc.go.ke) handles complaints. Because Ora keeps your data only on your device, you can exercise these yourself: Settings > Privacy (history, cookies, site permissions, saved passwords), Settings > Diagnostics (log) and \"What Ora has learned\". Uninstalling Ora deletes everything.",
    "No warranty." to "Ora is provided \"as is\", without warranties of any kind. To the extent the law allows, the developer is not responsible for loss or exposure of data while using it.",
)

fun noticeAsText(): String = "Ora: notice and privacy\n\n" + NoticeFull.joinToString("\n\n") { "${it.first} ${it.second}" }
