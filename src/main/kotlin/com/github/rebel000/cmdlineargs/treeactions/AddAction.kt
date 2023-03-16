package com.github.rebel000.cmdlineargs.treeactions

import com.github.rebel000.cmdlineargs.ui.ArgumentPropertiesDialog
import com.github.rebel000.cmdlineargs.ui.ArgumentTree
import com.github.rebel000.cmdlineargs.ui.ArgumentTreeNode
import com.intellij.ui.AnActionButton
import com.intellij.ui.AnActionButtonRunnable
import javax.swing.tree.TreePath

class AddAction(private val tree: ArgumentTree) : AnActionButtonRunnable {
    override fun run(button: AnActionButton) {
        val newNode = ArgumentTreeNode("", false)
        if (ArgumentPropertiesDialog(newNode).showAndGet()) {
            tree.addNode(newNode, tree.selectedNode())
            tree.expandPath(TreePath(newNode.parent!!.path))
        }
    }
}