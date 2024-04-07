package com.github.rebel000.cmdlineargs.treeactions

import com.github.rebel000.cmdlineargs.ArgumentsService
import com.github.rebel000.cmdlineargs.ui.*
import com.intellij.openapi.actionSystem.*

class ShowSharedAction : ToggleAction() {
    override fun isSelected(e: AnActionEvent): Boolean {
        val project = e.project ?: return false
        return ArgumentsService.getInstance(project).showSharedArgs
    }

    override fun setSelected(e: AnActionEvent, state: Boolean) {
        val argsService = ArgumentsService.getInstance(e.project ?: return)
        if (argsService.showSharedArgs) {
            argsService.showSharedArgs = false
        }
        else {
            if (SharedArgsWarningDialog(argsService).showAndGet()) {
                argsService.showSharedArgs = true
            }
        }
        argsService.saveState()
    }

    override fun update(e: AnActionEvent) {
        val argsService = ArgumentsService.getInstance(e.project ?: return)
        val toolWindow = e.dataContext.getData(PlatformDataKeys.TOOL_WINDOW)
        Toggleable.setSelected(e.presentation, argsService.showSharedArgs)
        e.presentation.isEnabled = toolWindow != null && toolWindow.id == ArgumentTreeView.COMMANDLINEARGS_TOOLWINDOW_ID
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}