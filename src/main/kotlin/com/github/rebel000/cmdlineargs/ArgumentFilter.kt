package com.github.rebel000.cmdlineargs

import com.github.rebel000.cmdlineargs.ui.ArgumentTreeNode
import com.intellij.execution.RunManager
import com.intellij.openapi.project.Project
import com.jetbrains.rider.projectView.SolutionConfigurationManager

open class ArgumentFilter(project: Project) {
    private val activeSolutionConfigurationAndPlatform =
        SolutionConfigurationManager.getInstance(project).activeConfigurationAndPlatform
    private val configuration = activeSolutionConfigurationAndPlatform?.configuration.orEmpty()
    private val platform = activeSolutionConfigurationAndPlatform?.platform.orEmpty()
    private val runConfiguration = RunManager.getInstance(project).selectedConfiguration?.name.orEmpty()

    fun check(node: ArgumentTreeNode): Boolean {
        var result = node.isChecked
        if (result && node.filters.configuration.isNotEmpty()) {
            val filters = node.filters.configuration.split(";")
            result = filters.find { filter -> checkPattern(configuration, filter) } != null
        }
        if (result && node.filters.platform.isNotEmpty()) {
            val filters = node.filters.platform.split(";")
            result = filters.find { filter -> checkPattern(platform, filter) } != null
        }
        if (result && node.filters.runConfiguration.isNotEmpty()) {
            val filters = node.filters.runConfiguration.split(";")
            result = filters.find { filter -> checkPattern(runConfiguration, filter) } != null
        }
        return result
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

    override fun equals(other: Any?): Boolean {
        if (other is ArgumentFilter) {
            return configuration == other.configuration
                    && platform == other.platform
                    && runConfiguration == other.runConfiguration
        }
        return super.equals(other)
    }

    override fun hashCode(): Int {
        var result = activeSolutionConfigurationAndPlatform?.hashCode() ?: 0
        result = 31 * result + configuration.hashCode()
        result = 31 * result + platform.hashCode()
        result = 31 * result + runConfiguration.hashCode()
        return result
    }
}