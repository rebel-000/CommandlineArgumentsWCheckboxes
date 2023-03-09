package com.github.rebel000.cmdlineargs

import com.intellij.openapi.project.Project
import com.jetbrains.rider.cpp.run.configurations.CppConfigurationParametersExtension
import com.jetbrains.rider.run.configurations.exe.ExeConfigurationParameters

@Suppress("unused")
class CppConfigurationParametersExtension(private val project: Project) : CppConfigurationParametersExtension {
    private val argsService = ArgumentTreeService.getInstance(project)

    override fun process(parameters: ExeConfigurationParameters) {
        if (argsService.shouldOverride) {
            parameters.programParameters = argsService.arguments
        }
        else {
            parameters.programParameters = parameters.programParameters + " " + argsService.arguments
        }
    }
}
