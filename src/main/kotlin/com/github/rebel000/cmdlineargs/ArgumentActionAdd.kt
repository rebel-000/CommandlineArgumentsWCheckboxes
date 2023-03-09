package com.github.rebel000.cmdlineargs

import com.intellij.openapi.project.Project
import com.intellij.ui.AnActionButton
import com.intellij.ui.AnActionButtonRunnable
import javax.swing.tree.TreePath

class ArgumentActionAdd(private val tree: ArgumentTree, private val project: Project): AnActionButtonRunnable {
    override fun run(button: AnActionButton) {
        val newNode = ArgumentTreeNode("", false)
        if (ArgumentNodePropertiesDialog(newNode).showAndGet()) {
            tree.addNode(newNode, tree.firstSelectedNode())
            tree.expandPath(TreePath((newNode.parent as ArgumentTreeNode).path))
        }
    }
}