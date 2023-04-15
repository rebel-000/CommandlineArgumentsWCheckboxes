package com.github.rebel000.cmdlineargs.extensions

import com.github.rebel000.cmdlineargs.ArgumentsService
import com.github.rebel000.cmdlineargs.COMMANDLINE_PATCHED
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.ProcessListener
import com.intellij.openapi.project.Project
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rider.run.PatchCommandLineExtension
import com.jetbrains.rider.run.WorkerRunInfo
import com.jetbrains.rider.runtime.DotNetRuntime
import org.jetbrains.concurrency.Promise
import org.jetbrains.concurrency.resolvedPromise

@Suppress("unused")
class PatchCommandLineExtension : PatchCommandLineExtension {
    override fun patchDebugCommandLine(
        lifetime: Lifetime,
        workerRunInfo: WorkerRunInfo,
        project: Project
    ): Promise<WorkerRunInfo> {
        return resolvedPromise(workerRunInfo)
    }

    override fun patchRunCommandLine(
        commandLine: GeneralCommandLine,
        dotNetRuntime: DotNetRuntime,
        project: Project
    ): ProcessListener? {
        val argsService = ArgumentsService.getInstance(project)
        if (argsService.shouldOverride || argsService.arguments.isNotEmpty()) {
            if (!commandLine.environment.contains(COMMANDLINE_PATCHED)) {
                if (argsService.shouldOverride) {
                    val defaultArgs = argsService.defaultArgs
                    if (defaultArgs.isNotEmpty()) {
                        val parameters = commandLine.parametersList.parameters
                        val expectedOffset = parameters.size - defaultArgs.size
                        var offset = expectedOffset
                        while (offset >= 0) {
                            var isValidOffset = true
                            for (j in defaultArgs.indices) {
                                if (defaultArgs[j] != parameters[offset + j]) {
                                    isValidOffset = false
                                    break
                                }
                            }
                            if (isValidOffset) {
                                break
                            }
                            offset--
                        }
                        if (offset > 0) {
                            val prefixArgs = parameters.subList(0, offset)
                            val suffixArgs = if (offset != expectedOffset) parameters.subList(
                                offset + defaultArgs.size,
                                parameters.size
                            ) else listOf<String>()
                            commandLine.parametersList.clearAll()
                            commandLine.addParameters(prefixArgs)
                            commandLine.addParameters(suffixArgs)
                        }
                    }
                }
                commandLine.addParameters(argsService.arguments)
            } else {
                commandLine.environment -= COMMANDLINE_PATCHED
            }
        }
        return null
    }
}
