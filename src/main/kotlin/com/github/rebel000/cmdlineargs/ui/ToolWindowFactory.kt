package com.github.rebel000.cmdlineargs.ui

import com.github.rebel000.cmdlineargs.Resources
import com.github.rebel000.cmdlineargs.treeactions.*
import com.intellij.icons.AllIcons
import com.intellij.ide.CommonActionsManager
import com.intellij.ide.DefaultTreeExpander
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.content.impl.ContentImpl
import com.intellij.util.ui.JBUI
import java.util.*

@Suppress("DialogTitleCapitalization")
class ToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = SimpleToolWindowPanel(true)
        val tree = ArgumentTree(project)
        panel.add(ToolbarDecorator.createDecorator(tree).apply {
            setToolbarPosition(ActionToolbarPosition.TOP)
            setPanelBorder(JBUI.Borders.empty())
            setAddAction(AddAction(tree))
            setRemoveAction(RemoveAction(tree))
            setEditAction(EditAction(tree))
            setMoveDownAction(MoveDownAction(tree))
            setMoveUpAction(MoveUpAction(tree))
            addExtraActions(
                Separator(),
                CopyAction(tree),
                PasteAction(tree),
                Separator(),
                OverrideBaseParameters(tree, project),
                ReloadAction(tree)
            )
        }.createPanel())
        tree.postInit()
        val treeExpander = DefaultTreeExpander(tree)
        val titleActions = LinkedList<AnAction>()
        titleActions.add(CommonActionsManager.getInstance().createExpandAllAction(treeExpander, tree))
        titleActions.add(CommonActionsManager.getInstance().createCollapseAllAction(treeExpander, tree))
        toolWindow.title = Resources.message("toolwindow.title")
        toolWindow.setTitleActions(titleActions)
        toolWindow.contentManager.addContent(ContentImpl(panel, "", true))
    }
}