package com.github.rebel000.cmdlineargs.treeactions

import com.github.rebel000.cmdlineargs.Resources
import com.github.rebel000.cmdlineargs.ui.ArgumentTree
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class ReloadAction(private val tree: ArgumentTree) :
    AnAction(Resources.message("action.reload"), Resources.message("action.reload"), AllIcons.Actions.Refresh) {
    override fun actionPerformed(e: AnActionEvent) {
        tree.reloadState()
    }
}