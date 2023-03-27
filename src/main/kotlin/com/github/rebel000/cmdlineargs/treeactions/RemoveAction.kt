package com.github.rebel000.cmdlineargs.treeactions

import com.github.rebel000.cmdlineargs.ui.ArgumentTree
import com.intellij.ui.AnActionButton
import com.intellij.ui.AnActionButtonRunnable

class RemoveAction(private val tree: ArgumentTree) : AnActionButtonRunnable {
    override fun run(button: AnActionButton) {
        val index: Int? = tree.selectionRows.firstOrNull()
        tree.removeNodes(tree.selectedNodes(false))
        if (index != null) {
            tree.addSelectionRow(index)
        }
    }
}