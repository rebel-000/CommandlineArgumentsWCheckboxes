package com.github.rebel000.cmdlineargs

import com.intellij.AbstractBundle
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey

@NonNls
private const val BUNDLE = "messages.CommandlineArgsBundle"

object Resources : AbstractBundle(BUNDLE) {
    @Suppress("SpreadOperator")
    @JvmStatic
    fun message(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any) = getMessage(key, *params)

    @Suppress("unused", "SpreadOperator")
    @JvmStatic
    fun messagePtr(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any) = run {
        message(key, *params)
    }
}
