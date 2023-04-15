package com.github.rebel000.cmdlineargs.treeactions

import com.github.rebel000.cmdlineargs.ui.ArgumentTree
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class RemoveAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val tree = ArgumentTree.getInstance(e.project) ?: return
        val index: Int? = tree.selectionRows?.firstOrNull()
        tree.removeNodes(tree.selectedNodes(false))
        if (index != null) {
            tree.addSelectionRow(index)
        }
    }
}