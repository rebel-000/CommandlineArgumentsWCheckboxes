package com.github.rebel000.cmdlineargs.ui

import com.github.rebel000.cmdlineargs.TOOLBAR_ACTION_ID
import com.intellij.ide.CommonActionsManager
import com.intellij.ide.DefaultTreeExpander
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionToolbarPosition
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.ToolbarDecorator
import com.intellij.util.ui.JBUI
import java.util.*

class ArgumentTreeView(project: Project) : SimpleToolWindowPanel(true) {
    val tree = ArgumentTree(project)

    init {
        add(ToolbarDecorator.createDecorator(tree).apply {
            setToolbarPosition(ActionToolbarPosition.TOP)
            setPanelBorder(JBUI.Borders.empty())
            addExtraActions(
                ActionManager.getInstance().getAction(TOOLBAR_ACTION_ID)
            )
        }.setForcedDnD().createPanel())
        tree.postInit()
    }

    fun getTitleActions(): List<AnAction> {
        val treeExpander = DefaultTreeExpander(tree)
        val titleActions = LinkedList<AnAction>()
        titleActions.add(CommonActionsManager.getInstance().createExpandAllAction(treeExpander, tree))
        titleActions.add(CommonActionsManager.getInstance().createCollapseAllAction(treeExpander, tree))
        return titleActions
    }
}