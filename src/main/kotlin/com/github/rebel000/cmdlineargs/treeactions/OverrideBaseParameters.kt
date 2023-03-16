package com.github.rebel000.cmdlineargs.treeactions

import com.github.rebel000.cmdlineargs.ArgumentsService
import com.github.rebel000.cmdlineargs.Resources
import com.github.rebel000.cmdlineargs.ui.ArgumentTree
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.ui.ToggleActionButton

class OverrideBaseParameters(private val tree: ArgumentTree, project: Project): ToggleActionButton(Resources.message("action.shouldOverride"), AllIcons.Actions.Lightning) {
    private val argsService = ArgumentsService.getInstance(project)

    override fun isSelected(e: AnActionEvent?): Boolean {
        return argsService.shouldOverride
    }

    override fun setSelected(e: AnActionEvent?, state: Boolean) {
        argsService.shouldOverride = state
        tree.saveState()
    }
}