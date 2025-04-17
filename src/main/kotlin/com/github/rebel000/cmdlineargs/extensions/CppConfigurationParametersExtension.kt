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
            val cfgName = parameters.envs[ArgumentsService.CPP_RUN_CONFIGURATION_NAME_ENV_VARIABLE] ?: argsService.lastCppConfigName
            parameters.programParameters += " " + argsService.buildArgs(cfgName)
            argsService.lastCppConfigName = null
        }
    }

    override fun dispose() {}
}
