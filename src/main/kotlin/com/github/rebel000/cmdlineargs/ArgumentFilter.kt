package com.github.rebel000.cmdlineargs

import com.github.rebel000.cmdlineargs.ui.ArgumentTreeNode

open class ArgumentFilter(private val configuration: String?, private val platform: String?, private val runConfiguration: String?) {
    fun check(node: ArgumentTreeNode): Boolean {
        return node.isChecked
            && checkFilter(configuration, node.filters.configuration)
            && checkFilter(platform, node.filters.platform)
            && checkFilter(runConfiguration, node.filters.runConfiguration)
    }
    
    private fun checkFilter(configField: String?, filterField: String): Boolean {
        if (configField != null && filterField.isNotEmpty()) {
            val filters = filterField.split(";")
            return filters.find { filter -> checkPattern(configField, filter) } != null
        }
        return true
    }

    private fun checkPattern(str: String, pattern: String): Boolean {
        if (pattern.startsWith("*")) {
            if (pattern.endsWith("*")) {
                return str.contains(pattern.trim('*'))
            }
            return str.endsWith(pattern.trim('*'))
        } else if (pattern.endsWith("*")) {
            return str.startsWith(pattern.trim('*'))
        }

        return str == pattern
    }
}