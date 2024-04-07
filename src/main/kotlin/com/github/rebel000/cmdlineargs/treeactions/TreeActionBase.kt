package com.github.rebel000.cmdlineargs.treeactions

import com.github.rebel000.cmdlineargs.ui.ArgumentTreeView
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys

abstract class TreeActionBase : AnAction() {
    override fun update(e: AnActionEvent) {
        val toolWindow = e.dataContext.getData(PlatformDataKeys.TOOL_WINDOW)
        e.presentation.isEnabled = toolWindow != null && toolWindow.id == ArgumentTreeView.COMMANDLINEARGS_TOOLWINDOW_ID
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}