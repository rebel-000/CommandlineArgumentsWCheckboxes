package com.github.rebel000.cmdlineargs

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

@Service
class ArgumentTreeService(private val project: Project) {
    companion object {
        const val COMMANDLINE_TREE_ARGUMENTS = "com.github.rebel000.cmdlineargs"
        fun getInstance(project: Project): ArgumentTreeService = project.service()
    }

    var arguments: String
        get() = PropertiesComponent.getInstance(project).getValue(COMMANDLINE_TREE_ARGUMENTS) ?: ""
        set(value) { PropertiesComponent.getInstance(project).setValue(COMMANDLINE_TREE_ARGUMENTS, value) }

    var shouldOverride: Boolean = false
}