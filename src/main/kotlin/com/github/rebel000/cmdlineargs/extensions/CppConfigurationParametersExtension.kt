package com.github.rebel000.cmdlineargs.extensions

import com.github.rebel000.cmdlineargs.ArgumentsService
import com.intellij.openapi.project.Project
import com.jetbrains.rider.cpp.run.configurations.CppConfigurationParametersExtension
import com.jetbrains.rider.run.configurations.exe.ExeConfigurationParameters

@Suppress("unused")
class CppConfigurationParametersExtension(project: Project) : CppConfigurationParametersExtension {
    private val argsService = ArgumentsService.getInstance(project)

    override fun process(parameters: ExeConfigurationParameters) {
        if (argsService.shouldOverride || argsService.arguments.isNotEmpty()) {
            if (argsService.shouldOverride) {
                parameters.programParameters = argsService.argumentsString
            } else {
                parameters.programParameters = parameters.programParameters + " " + argsService.argumentsString
            }
        }
    }
}
