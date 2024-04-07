package com.github.rebel000.cmdlineargs.treeactions

import com.github.rebel000.cmdlineargs.ui.ArgumentTree
import com.github.rebel000.cmdlineargs.ui.ArgumentTreeNode
import com.intellij.openapi.actionSystem.AnActionEvent
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreePath

class RenamePrevAction : TreeActionBase() {
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
        val index = parent.getIndex(node) - 1
        if (index < 0) {
            rename(tree, parent)
            return
        }
        val sibling = parent.getChildAt(index) as ArgumentTreeNode
        if (sibling.isFolder && sibling.isExpanded) {
            rename(tree, sibling.lastChild as ArgumentTreeNode)
            return
        }
        rename(tree, sibling)
    }
    
    private fun rename(tree: ArgumentTree, node: DefaultMutableTreeNode) {
        tree.stopEditing()
        val path = TreePath(node.path)
        tree.selectionPaths = arrayOf(path)
        tree.startEditingAtPath(path)
    }
}