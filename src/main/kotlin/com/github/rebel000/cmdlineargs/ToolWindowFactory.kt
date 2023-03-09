package com.github.rebel000.cmdlineargs

import com.intellij.icons.AllIcons
import com.intellij.ide.CommonActionsManager
import com.intellij.ide.DefaultTreeExpander
import com.intellij.openapi.actionSystem.ActionToolbarPosition
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.ToggleActionButton
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.content.impl.ContentImpl
import java.util.*

@Suppress("DialogTitleCapitalization")
class ToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val argsService = ArgumentTreeService.getInstance(project)
        val panel = SimpleToolWindowPanel(true)
        val tree = ArgumentTree(project)

        panel.add(ToolbarDecorator.createDecorator(tree).apply {
            this.setToolbarPosition(ActionToolbarPosition.TOP)
            this.setAddAction(ArgumentActionAdd(tree, project))
            this.setRemoveAction(ArgumentActionRemove(tree, project))
            this.setMoveDownAction(ArgumentActionMoveDown(tree, project))
            this.setMoveUpAction(ArgumentActionMoveUp(tree, project))
            val extraActionsGroup = DefaultActionGroup()
            extraActionsGroup.add(object: ToggleActionButton(Resources.message("action.shouldOverride"), AllIcons.Actions.EditSource) {
                override fun isSelected(e: AnActionEvent?): Boolean {
                    return argsService.shouldOverride
                }
                override fun setSelected(e: AnActionEvent?, state: Boolean) {
                    argsService.shouldOverride = state
                }
            })
            this.setActionGroup(extraActionsGroup)
        }.createPanel())

        val treeExpander = DefaultTreeExpander(tree)
        val titleActions = LinkedList<AnAction>()
        titleActions.add(CommonActionsManager.getInstance().createExpandAllAction(treeExpander, tree))
        titleActions.add(CommonActionsManager.getInstance().createCollapseAllAction(treeExpander, tree))

        toolWindow.title = Resources.message("toolwindow.title")
        toolWindow.setIcon(AllIcons.Debugger.Console)
        toolWindow.setTitleActions(titleActions);
        toolWindow.contentManager.addContent(ContentImpl(panel, "", true))
    }
}