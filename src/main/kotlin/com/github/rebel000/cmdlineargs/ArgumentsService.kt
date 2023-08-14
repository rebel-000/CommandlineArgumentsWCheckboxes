package com.github.rebel000.cmdlineargs

import com.github.rebel000.cmdlineargs.ui.ArgumentTree
import com.github.rebel000.cmdlineargs.ui.ArgumentTreeRootNode
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.jetbrains.rider.projectView.solutionDirectory
import com.jetbrains.rider.projectView.solutionName
import java.io.File
import java.util.*

@Service(Service.Level.PROJECT)
class ArgumentsService(private val project: Project) {
    private val settingsFileName: String = project.solutionDirectory.path + "/" + project.solutionName + ".ddargs.json"
    val rootNode = ArgumentTreeRootNode()
    var arguments: List<String> = listOf()
    var shouldOverride: Boolean = false
    val tree: ArgumentTree?
        get() = ArgumentTree.getInstance(project)

    init {
        reloadState()
    }

    companion object {
        fun getInstance(project: Project): ArgumentsService = project.service()
    }

    val argumentsString: String
        get() {
            return arguments.joinToString(" ")
        }

    fun saveState() {
        val jObject = JsonObject()
        val items = JsonArray(rootNode.childCount)
        for (child in rootNode.childrenArgs()) {
            items.add(child.toJson())
        }
        jObject.addProperty("version", 1)
        jObject.addProperty("override", shouldOverride)
        jObject.add("items", items)
        File(settingsFileName).writeText(jObject.toString())
    }

    fun reloadState() {
        tree?.lock()
        val settingsFile = File(settingsFileName)
        if (settingsFile.exists()) {
            val jsonString = settingsFile.readText()
            val jObject = JsonParser.parseString(jsonString).asJsonObject
            val version = jObject.get("version")
            if (version != null && version.asInt == 1) {
                rootNode.removeAllChildren()
                shouldOverride = jObject.get("override")?.asBoolean == true
                rootNode.fromJson(jObject)
            }
        }
        tree?.postLoad()
        tree?.unlock()
        rebuildArgs()
    }

    fun rebuildArgs() {
        arguments = Vector()
        rootNode.getArgs(arguments as Vector<String>, ArgumentFilter(project))
    }
}