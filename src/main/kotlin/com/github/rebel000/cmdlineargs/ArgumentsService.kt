package com.github.rebel000.cmdlineargs

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

@Service
class ArgumentsService(private val project: Project) {
    var arguments: List<String> = listOf()
    var shouldOverride: Boolean = false

    companion object {
        fun getInstance(project: Project): ArgumentsService = project.service()
    }

    val argumentsString: String
        get() {
            return arguments.joinToString(" ")
        }
}