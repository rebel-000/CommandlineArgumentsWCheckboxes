package icons.com.github.rebel000.cmdlineargs.treeactions

import com.github.rebel000.cmdlineargs.TOOLWINDOW_ID
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys

abstract class TreeActionBase : AnAction() {
    override fun update(e: AnActionEvent) {
        val toolWindow = e.dataContext.getData(PlatformDataKeys.TOOL_WINDOW)
        e.presentation.isEnabled = toolWindow != null && toolWindow.id == TOOLWINDOW_ID
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}