package com.github.rebel000.cmdlineargs.treeactions

import com.github.rebel000.cmdlineargs.ArgumentsService
import com.github.rebel000.cmdlineargs.Resources
import com.github.rebel000.cmdlineargs.ui.ArgumentTree
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.ui.ToggleActionButton

class OverrideBaseParameters :
    ToggleActionButton(Resources.message("action.cmdlineargs.shouldOverride.text"), AllIcons.Actions.Lightning) {
    override fun isSelected(e: AnActionEvent?): Boolean {
        val project = (e ?: return false).project ?: return false
        return ArgumentsService.getInstance(project).shouldOverride
    }

    override fun setSelected(e: AnActionEvent?, state: Boolean) {
        val project = (e ?: return).project ?: return
        ArgumentsService.getInstance(project).shouldOverride = state
        ArgumentTree.getInstance(project)?.saveState()
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}