package com.github.rebel000.cmdlineargs.treeactions

import com.github.rebel000.cmdlineargs.ArgumentsService
import com.github.rebel000.cmdlineargs.TOOLWINDOW_ID
import com.github.rebel000.cmdlineargs.ui.ArgumentTree
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.actionSystem.ToggleAction

class OverrideBaseParameters : ToggleAction() {
    override fun isSelected(e: AnActionEvent): Boolean {
        val project = e.project ?: return false
        return ArgumentsService.getInstance(project).shouldOverride
    }

    override fun setSelected(e: AnActionEvent, state: Boolean) {
        val argsService = ArgumentsService.getInstance(e.project ?: return)
        argsService.shouldOverride = state
        argsService.saveState()
    }

    override fun update(e: AnActionEvent) {
        val toolWindow = e.dataContext.getData(PlatformDataKeys.TOOL_WINDOW)
        e.presentation.isEnabled = toolWindow != null && toolWindow.id == TOOLWINDOW_ID
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}