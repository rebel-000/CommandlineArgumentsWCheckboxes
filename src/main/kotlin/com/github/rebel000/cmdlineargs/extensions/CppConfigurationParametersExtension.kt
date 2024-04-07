package com.github.rebel000.cmdlineargs.extensions

import com.github.rebel000.cmdlineargs.ArgumentsService
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.jetbrains.rider.cpp.run.configurations.CppConfigurationParametersExtension
import com.jetbrains.rider.run.configurations.exe.ExeConfigurationParameters

@Suppress("unused")
class CppConfigurationParametersExtension(project: Project) : CppConfigurationParametersExtension, Disposable {
    private val argsService = ArgumentsService.getInstance(project)

    override fun process(parameters: ExeConfigurationParameters) {
        if (argsService.isEnabled) {
            parameters.programParameters = argsService.buildArgs()
        }
    }

    override fun dispose() {
    }
}
