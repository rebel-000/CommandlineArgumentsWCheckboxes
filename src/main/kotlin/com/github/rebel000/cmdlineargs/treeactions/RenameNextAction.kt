package com.github.rebel000.cmdlineargs.treeactions

import com.github.rebel000.cmdlineargs.ui.ArgumentTree
import com.github.rebel000.cmdlineargs.ui.ArgumentTreeNode
import com.intellij.openapi.actionSystem.AnActionEvent
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreePath

class RenameNextAction : TreeActionBase() {
    override fun actionPerformed(e: AnActionEvent) {
        val tree = ArgumentTree.getInstance(e.project) ?: return
        if (!tree.isEditing) {
            return
        }
        val node = (tree.editingPath?.lastPathComponent) as ArgumentTreeNode?
        val parent = node?.parent
        if (parent !is ArgumentTreeNode) {
            tree.stopEditing()
            return
        }
        if (node.isFolder && node.isExpanded) {
            rename(tree, node.firstChild as ArgumentTreeNode)
            return
        }
        val index = parent.getIndex(node) + 1
        if (index != parent.childCount) {
            val sibling = parent.getChildAt(index) as ArgumentTreeNode
            rename(tree, sibling)
            return
        }
        val sibling = parent.nextSibling
        if (sibling != null) {
            rename(tree, sibling)
        }
        else {
            val newNode = ArgumentTreeNode("", isFolder = false, readonly = false)
            tree.addNode(newNode, tree.selectedNode())
            rename(tree, newNode)
        }
    }

    private fun rename(tree: ArgumentTree, node: DefaultMutableTreeNode) {
        tree.stopEditing()
        val path = TreePath(node.path)
        tree.selectionPaths = arrayOf(path)
        tree.startEditingAtPath(path)
    }
}