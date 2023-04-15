package com.github.rebel000.cmdlineargs.extensions

import com.github.rebel000.cmdlineargs.ArgumentsService
import com.github.rebel000.cmdlineargs.COMMANDLINE_PATCHED
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.project.Project
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rider.run.configurations.RiderConfigurationExecutorExtension
import com.jetbrains.rider.run.configurations.RuntimeHotReloadRunConfigurationInfo
import com.jetbrains.rider.run.configurations.project.DotNetProjectConfigurationParameters
import com.jetbrains.rider.runtime.DotNetExecutable

@Suppress("unused")
class RiderConfigurationExecutorExtension : RiderConfigurationExecutorExtension {
    private var nextExecutor: RiderConfigurationExecutorExtension? = null
    override suspend fun canExecute(
        lifetime: Lifetime,
        hotReloadRunInfo: RuntimeHotReloadRunConfigurationInfo
    ): Boolean {
        val argsService = ArgumentsService.getInstance(hotReloadRunInfo.project)
        if (argsService.shouldOverride || argsService.arguments.isNotEmpty()) {
            val extensions = RiderConfigurationExecutorExtension.EP_NAME.getExtensions(hotReloadRunInfo.project)
            nextExecutor = extensions.find { ext -> ext != this && ext.canExecute(lifetime, hotReloadRunInfo) }
            return true
        }
        return false
    }

    override fun executor(
        project: Project,
        environment: ExecutionEnvironment,
        parameters: DotNetProjectConfigurationParameters
    ): DotNetExecutable {
        val argsService = ArgumentsService.getInstance(project)
        val parametersCopy = parameters.copy()
        parametersCopy.envs += Pair(COMMANDLINE_PATCHED, "1")
        if (argsService.shouldOverride) {
            parametersCopy.programParameters = argsService.argumentsString
        } else {
            parametersCopy.programParameters = parametersCopy.programParameters + " " + argsService.argumentsString
        }
        val executable = nextExecutor?.executor(project, environment, parametersCopy)
        nextExecutor = null
        return executable ?: parametersCopy.toDotNetExecutable()
    }
}
