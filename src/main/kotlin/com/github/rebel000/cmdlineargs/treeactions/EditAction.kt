package com.github.rebel000.cmdlineargs.treeactions

import com.github.rebel000.cmdlineargs.ui.ArgumentTree
import com.intellij.ui.AnActionButton
import com.intellij.ui.AnActionButtonRunnable

class EditAction(private val tree: ArgumentTree) : AnActionButtonRunnable {
    override fun run(button: AnActionButton) {
        val node = tree.selectedNode()
        if (node != null) {
            tree.editNode(node)
        }
    }
}