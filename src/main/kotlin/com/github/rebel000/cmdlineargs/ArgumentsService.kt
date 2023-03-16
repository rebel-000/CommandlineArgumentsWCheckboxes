package com.github.rebel000.cmdlineargs

import com.intellij.execution.RunManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.util.execution.ParametersListUtil
import com.jetbrains.rider.cpp.run.configurations.CppProjectConfiguration
import com.jetbrains.rider.projectView.SolutionConfigurationManager
import com.jetbrains.rider.run.configurations.project.DotNetProjectConfiguration

@Service
class ArgumentsService(private val project: Project) {
    var arguments: List<String> = listOf()
    var shouldOverride: Boolean = false

    companion object {
        fun getInstance(project: Project): ArgumentsService = project.service()
    }

    val defaultArgs: Array<String> get() {
        val solutionConfiguration = SolutionConfigurationManager.getInstance(project).activeConfigurationAndPlatform
        if (solutionConfiguration != null) {
            val runManager = RunManager.getInstance(project)
            val selectedConfiguration = runManager.selectedConfiguration?.configuration
            if (selectedConfiguration is CppProjectConfiguration) {
                val params = selectedConfiguration.parameters.parametersMap.getParametersForConfigurationAndPlatform(
                    solutionConfiguration.configuration,
                    solutionConfiguration.platform,
                    "")
                return ParametersListUtil.parseToArray(params.programParameters)
            }
            if (selectedConfiguration is DotNetProjectConfiguration) {
                return ParametersListUtil.parseToArray(selectedConfiguration.parameters.programParameters)
            }
        }
        return arrayOf()
    }

    val argumentsString: String get() {
        return arguments.joinToString(" ")
    }
}