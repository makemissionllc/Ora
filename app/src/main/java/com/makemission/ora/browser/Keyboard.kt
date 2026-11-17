package com.makemission.ora.browser

import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager

/**
 * Closes the on-screen keyboard for this window. The Compose controller alone can leave it open
 * when the focused field is re-attached during a page load, so the input method is asked directly too.
 */
fun View.hideKeyboard() {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
    imm?.hideSoftInputFromWindow(windowToken, 0)
}
