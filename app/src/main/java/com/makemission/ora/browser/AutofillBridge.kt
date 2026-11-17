package com.makemission.ora.browser

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.View
import android.view.ViewGroup
import android.view.ViewStructure
import android.widget.FrameLayout
import com.makemission.ora.log.BrowserLog
import org.mozilla.geckoview.GeckoView

/**
 * Passes the page's login fields to Android's autofill framework.
 *
 * To build the structure for the autofill service, the framework walks the window from the top. Compose's root view
 * reports a virtual child of its own, and the walk then skips the real views beneath it, so the GeckoView inside
 * Compose is never reached. This view sits beside Compose in the window, where the walk does reach it, and adds the
 * current page's fields to the walk.
 */
internal class AutofillBridgeView(
    context: Context,
    private val page: () -> GeckoView?,
) : View(context) {
    init {
        importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_YES
    }

    override fun onProvideAutofillVirtualStructure(structure: ViewStructure, flags: Int) {
        super.onProvideAutofillVirtualStructure(structure, flags)
        val geckoView = page() ?: return
        val index = structure.addChildCount(1)
        geckoView.dispatchProvideAutofillStructure(structure.newChild(index), flags)
    }
}

/**
 * Adds an [AutofillBridgeView] to the window's content and returns the function that removes it again.
 * Returns null, and logs why, when there is no window to attach to.
 */
internal fun attachAutofillBridge(context: Context, page: () -> GeckoView?): (() -> Unit)? {
    val activity = context.findHostActivity()
    val content = activity?.findViewById<ViewGroup>(android.R.id.content)
    if (activity == null || content == null) {
        BrowserLog.warn("autofill", "Autofill cannot read this page's fields: no window to attach to")
        return null
    }
    val bridge = AutofillBridgeView(activity, page)
    content.addView(bridge, FrameLayout.LayoutParams(1, 1))
    BrowserLog.info("autofill", "Autofill bridge attached to the window")
    return { content.removeView(bridge) }
}

private tailrec fun Context.findHostActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findHostActivity()
    else -> null
}
