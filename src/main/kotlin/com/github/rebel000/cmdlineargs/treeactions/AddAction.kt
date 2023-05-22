package com.github.rebel000.cmdlineargs.treeactions

import com.github.rebel000.cmdlineargs.ui.ArgumentPropertiesDialog
import com.github.rebel000.cmdlineargs.ui.ArgumentTree
import com.github.rebel000.cmdlineargs.ui.ArgumentTreeNode
import com.intellij.openapi.actionSystem.AnActionEvent
import icons.com.github.rebel000.cmdlineargs.treeactions.TreeActionBase
import javax.swing.tree.TreePath

class AddAction : TreeActionBase() {
    override fun actionPerformed(e: AnActionEvent) {
        val tree = ArgumentTree.getInstance(e.project) ?: return
        val newNode = ArgumentTreeNode("", false)
        if (ArgumentPropertiesDialog(newNode).showAndGet()) {
            tree.addNode(newNode, tree.selectedNode())
            tree.expandPath(TreePath(newNode.parent!!.path))
        }
    }
}