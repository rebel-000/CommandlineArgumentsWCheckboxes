package com.github.rebel000.cmdlineargs

import com.github.rebel000.cmdlineargs.ui.ArgumentTree
import com.github.rebel000.cmdlineargs.ui.ArgumentTreeRootNode
import com.github.rebel000.cmdlineargs.ui.NotSupportedNode
import com.google.gson.JsonParser
import com.intellij.execution.*
import com.intellij.execution.compound.CompoundRunConfiguration
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.multilaunch.MultiLaunchConfiguration
import com.intellij.execution.multilaunch.execution.executables.impl.RunConfigurationExecutableManager
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.jetbrains.rider.cpp.run.configurations.CppProjectConfiguration
import com.jetbrains.rider.cpp.run.configurations.launch.LocalCppProjectLaunchParameters
import com.jetbrains.rider.cpp.run.configurations.rdjson.RdJsonLaunchParameters
import com.jetbrains.rider.projectView.solution
import com.jetbrains.rider.projectView.solutionDirectory
import com.jetbrains.rider.projectView.solutionName
import com.jetbrains.rider.run.configurations.dotNetExe.DotNetExeConfiguration
import com.jetbrains.rider.run.configurations.exe.ExeConfiguration
import com.jetbrains.rider.run.configurations.launchSettings.LaunchSettingsConfiguration
import com.jetbrains.rider.run.configurations.method.DotNetStaticMethodConfiguration
import com.jetbrains.rider.run.configurations.multiPlatform.RiderMultiPlatformConfiguration
import com.jetbrains.rider.run.configurations.project.DotNetProjectConfiguration
import com.jetbrains.rider.run.configurations.uwp.UwpConfiguration
import java.awt.datatransfer.StringSelection
import java.io.File
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit


@Service(Service.Level.PROJECT)
class ArgumentsService(private val project: Project) : Disposable {
    private val settingsFileName: String = project.solutionDirectory.path + "/" + project.solutionName + ".ddargs.json"
    private val sharedArgsStorage: SharedArgsStorage = ApplicationManager.getApplication().getService(SharedArgsStorage::class.java)
    private val notSupportedNode = NotSupportedNode()
    private var isNotSupportedVisible: Boolean = false
    private var isSharedVisible: Boolean = false
    private val saveDelayMs: Long = 500
    private val executorService: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
    private var scheduledSaveTask: ScheduledFuture<*>? = null
    val sharedRoot = ArgumentTreeRootNode(Resources.message("toolwindow.sharedArgumentsNode"))
    val localRoot = ArgumentTreeRootNode(Resources.message("toolwindow.rootNode"))
    val rootNode = ArgumentTreeRootNode(Resources.message("toolwindow.rootNode")).apply { add(localRoot) }
    var isEnabled: Boolean = true
    val tree: ArgumentTree?
        get() = ArgumentTree.getInstance(project)

    var showSharedArgs: Boolean
        get() = isSharedVisible
        set(value) = showSharedNode(value)

    var copyCmdArgsActions: Array<AnAction> = emptyArray()

    companion object {
        const val CPP_RUN_CONFIGURATION_NAME_ENV_VARIABLE = "com.github.rebel000.cmdlineargs:run_config_name"
        fun getInstance(project: Project): ArgumentsService = project.service()
    }

    init {
        reloadState()

        val connection = project.messageBus.connect(this)
        connection.subscribe(ExecutionManager.EXECUTION_TOPIC, object : ExecutionListener {
            override fun processStartScheduled(executorId: String, env: ExecutionEnvironment) {
                patchArguments(env.runnerAndConfigurationSettings)
            }

            override fun processNotStarted(executorId: String, env: ExecutionEnvironment) {
                cleanupArguments(env.runnerAndConfigurationSettings)
            }

            override fun processStarted(executorId: String, env: ExecutionEnvironment, handler: ProcessHandler) {
                cleanupArguments(env.runnerAndConfigurationSettings)
            }

            override fun processTerminating(executorId: String, env: ExecutionEnvironment, handler: ProcessHandler) {
                cleanupArguments(env.runnerAndConfigurationSettings)
            }
        })
        connection.subscribe(RunManagerListener.TOPIC, object : RunManagerListener {
            override fun runConfigurationSelected(settings: RunnerAndConfigurationSettings?) {
                showNotSupported(!isConfigSupported(settings))
            }

            override fun runConfigurationAdded(settings: RunnerAndConfigurationSettings) {
                showNotSupported(!isConfigSupported(settings))
                rebuildCopyArgsActions()
            }

            override fun runConfigurationRemoved(settings: RunnerAndConfigurationSettings) {
                rebuildCopyArgsActions()
            }

            override fun runConfigurationChanged(settings: RunnerAndConfigurationSettings) {
                rebuildCopyArgsActions()
            }
        })

        ApplicationManager.getApplication().invokeLater {
            showNotSupported(!isConfigSupported(RunManager.getInstance(project).selectedConfiguration))
            rebuildCopyArgsActions()
        }
    }

    override fun dispose() {
        scheduledSaveTask?.cancel(false)
        scheduledSaveTask = null
        executorService.shutdown()
        performSaveState()
    }

    fun buildArgs(runConfigurationName: String?): String {
        val arguments = Vector<String>()
        val runConfiguration = runConfigurationName ?: RunManager.getInstance(project).selectedConfiguration?.name
        val configurationAndPlatform = project.solution.solutionProperties.activeConfigurationPlatform.value
        rootNode.getArgs(arguments, ArgumentFilter(configurationAndPlatform?.configuration, configurationAndPlatform?.platform, runConfiguration))
        return arguments.joinToString(" ")
    }

    @Synchronized
    fun scheduleSaveState() {
        scheduledSaveTask?.cancel(false)
        scheduledSaveTask = executorService.schedule({ this.performSaveState() }, saveDelayMs, TimeUnit.MILLISECONDS)
    }

    @Synchronized
    private fun performSaveState() {
        val jObject = localRoot.toJson()
        jObject.addProperty("version", 1)
        jObject.addProperty("showShared", isSharedVisible)
        jObject.addProperty("isEnabled", isEnabled)
        File(settingsFileName).writeText(jObject.toString())
        if (isSharedVisible) {
            val sharedJObject = sharedRoot.toJson()
            sharedJObject.addProperty("version", 1)
            sharedArgsStorage.state.sharedArgs = sharedJObject.toString()
        }
        scheduledSaveTask = null
    }

    fun reloadState() {
        showSharedNode(false)
        var showShared = false
        val settingsFile = File(settingsFileName)
        if (settingsFile.exists()) {
            val jsonString = settingsFile.readText()
            val jObject = JsonParser.parseString(jsonString).asJsonObject
            val version = jObject.get("version")
            if (version != null && version.asInt == 1) {
                showShared = jObject.get("showShared")?.asBoolean == true
                isEnabled = jObject.get("isEnabled")?.asBoolean ?: true
                localRoot.fromJson(jObject)
            }
        }
        tree?.postLoad()
        showSharedNode(showShared)
    }

    private fun showNotSupported(enabled: Boolean) {
        if (isNotSupportedVisible != enabled) {
            val tree = tree
            if (enabled) {
                tree?.addNode(notSupportedNode)
                    ?: rootNode.insert(notSupportedNode, 0)
            }
            else {
                val model = tree?.model as? ArgumentTree.ArgumentTreeModel
                model?.removeNodeFromParent(notSupportedNode)
                    ?: notSupportedNode.removeFromParent()
            }
            isNotSupportedVisible = enabled
        }
    }

    private fun showSharedNode(enabled: Boolean) {
        if (isSharedVisible != enabled) {
            val tree = tree
            if (enabled) {
                val jObject = JsonParser.parseString(sharedArgsStorage.state.sharedArgs ?: "{}").asJsonObject
                val version = jObject.get("version")
                if (version != null && version.asInt == 1) {
                    sharedRoot.fromJson(jObject)
                }
                tree?.insertNode(sharedRoot, rootNode, rootNode.getIndex(localRoot))
                    ?: rootNode.insert(sharedRoot, rootNode.getIndex(localRoot))
                tree?.restoreExpandState(sharedRoot)
            } else {
                val model = tree?.model as? ArgumentTree.ArgumentTreeModel
                model?.removeNodeFromParent(sharedRoot)
                    ?: sharedRoot.removeFromParent()
            }
            isSharedVisible = enabled
        }
    }

    private fun rebuildCopyArgsActions() {
        val actions = mutableListOf<AnAction>()
        val runConfigs = RunManager.getInstance(project).allConfigurationsList.filter { isConfigSupported(it) }.map { it.name }
        runConfigs.forEach {
            actions.add(object : AnAction(it) {
                override fun actionPerformed(e: AnActionEvent) {
                    CopyPasteManager.getInstance().setContents(StringSelection(buildArgs(it)))
                }
            })
        }
        copyCmdArgsActions = actions.toTypedArray()
    }

    fun isConfigSupported(cfg: RunConfiguration?): Boolean {
        return (cfg is CppProjectConfiguration
            || cfg is UwpConfiguration
            || cfg is DotNetExeConfiguration
            || cfg is DotNetProjectConfiguration
            || cfg is ExeConfiguration
            || cfg is LaunchSettingsConfiguration
            || cfg is DotNetStaticMethodConfiguration
            || cfg is RiderMultiPlatformConfiguration)
    }

    private fun isConfigSupported(settings: RunnerAndConfigurationSettings?): Boolean {
        val cfg = (settings ?: return false).configuration
        if (cfg is CompoundRunConfiguration) {
            for (c in cfg.getConfigurationsWithEffectiveRunTargets()) {
                if (isConfigSupported(c.configuration)) {
                    return true
                }
            }
        }
        else if (cfg is MultiLaunchConfiguration) {
            for (d in cfg.descriptors) {
                val executable = d.executable;
                if (executable is RunConfigurationExecutableManager.RunConfigurationExecutable 
                    && isConfigSupported(executable.settings.configuration)) {
                    return true
                }
            }
        }
        return isConfigSupported(cfg)
    }

    private fun patchArguments(runnerAndConfigurationSettings: RunnerAndConfigurationSettings?): Boolean {
        if (!isEnabled) {
            return false
        }
        when (val cfg = runnerAndConfigurationSettings?.configuration) {
            is CppProjectConfiguration -> {
                val (config, platform) = project.solution.solutionProperties.activeConfigurationPlatform.value ?: return false
                if (cfg.parameters.parametersMap.hasParametersForConfigurationAndPlatform(config, platform)) {
                    val activeParameters = cfg.parameters.parametersMap.getParametersForConfigurationAndPlatform(config, platform, cfg.parameters.defaultProjectFilePath)
                    val launchParameters = activeParameters.getCurrentLaunchParameters()
                    if (launchParameters is LocalCppProjectLaunchParameters) {
                        launchParameters.envs[CPP_RUN_CONFIGURATION_NAME_ENV_VARIABLE] = cfg.name
                    }
                    else if (launchParameters is RdJsonLaunchParameters) {
                        launchParameters.envs[CPP_RUN_CONFIGURATION_NAME_ENV_VARIABLE] = cfg.name
                    }
                }
            }

            is UwpConfiguration -> {
                cfg.uwpParameters.programParameters = buildArgs(cfg.name)
                return true
            }

            is DotNetExeConfiguration -> {
                cfg.parameters.programParameters = buildArgs(cfg.name)
                return true
            }

            is DotNetProjectConfiguration -> {
                cfg.parameters.programParameters = buildArgs(cfg.name)
                return true
            }

            is ExeConfiguration -> {
                cfg.parameters.programParameters = buildArgs(cfg.name)
                return true
            }

            is LaunchSettingsConfiguration -> {
                cfg.parameters.runtimeArguments = buildArgs(cfg.name)
                return true
            }

            is DotNetStaticMethodConfiguration -> {
                cfg.parameters.programParameters = buildArgs(cfg.name)
                return true
            }

            is RiderMultiPlatformConfiguration -> {
                cfg.parameters.programParameters = buildArgs(cfg.name)
                return true
            }

            else -> {
                return false
            }
        }

        return false
    }

    private fun cleanupArguments(runnerAndConfigurationSettings: RunnerAndConfigurationSettings?) {
        if (isEnabled) {
            val cfg = runnerAndConfigurationSettings?.configuration
            if (cfg is CppProjectConfiguration) {
                val (config, platform) = project.solution.solutionProperties.activeConfigurationPlatform.value ?: return
                if (cfg.parameters.parametersMap.hasParametersForConfigurationAndPlatform(config, platform)) {
                    val activeParameters = cfg.parameters.parametersMap.getParametersForConfigurationAndPlatform(config, platform, cfg.parameters.defaultProjectFilePath)
                    val launchParameters = activeParameters.getCurrentLaunchParameters()
                    if (launchParameters is LocalCppProjectLaunchParameters) {
                        launchParameters.envs -= CPP_RUN_CONFIGURATION_NAME_ENV_VARIABLE
                    }
                    else if (launchParameters is RdJsonLaunchParameters) {
                        launchParameters.envs -= CPP_RUN_CONFIGURATION_NAME_ENV_VARIABLE
                    }
                }
            }
        }
    }
}