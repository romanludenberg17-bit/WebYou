package com.webyou.browser

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.Modifier

/** Compatibility bridge for MainActivity's package-local horizontalScroll calls. */
fun Modifier.horizontalScroll(state: ScrollState, reverseScrolling: Boolean = false): Modifier =
    androidx.compose.foundation.horizontalScroll(this, state, reverseScrolling)
