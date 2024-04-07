package com.github.rebel000.cmdlineargs.treeactions

import com.github.rebel000.cmdlineargs.ui.ArgumentTree
import com.github.rebel000.cmdlineargs.ui.ArgumentTreeNode
import com.intellij.openapi.actionSystem.AnActionEvent
import javax.swing.tree.TreePath

class MoveUpAction : TreeActionBase() {
    override fun actionPerformed(e: AnActionEvent) {
        val tree = ArgumentTree.getInstance(e.project) ?: return
        var selectedNodes = tree.selectedNodes(true)
        if (selectedNodes.isEmpty()) {
            return
        }
        selectedNodes = selectedNodes.filter { !it.checkIsAncestorIn(selectedNodes) && !it.readonly }
        var anchorNode = selectedNodes.first()
        var parent = anchorNode.parent
        if (parent !is ArgumentTreeNode) {
            return
        }
        var index = parent.getIndex(anchorNode) - 1
        if (index == -2) {
            return
        }
        if (index == -1) {
            anchorNode = parent
            parent = parent.parent
            if (parent !is ArgumentTreeNode) {
                return
            }
        }
        if (index != -1 || anchorNode.readonly) {
            val neighbor = parent.getChildBefore(anchorNode) as ArgumentTreeNode?
            if (neighbor?.isFolder == true) {
                parent = neighbor
                index = neighbor.childCount
            } else if (anchorNode.readonly) {
                return
            }
        }
        else {
            index = parent.getIndex(anchorNode)
        }
        val newSelectionPaths = ArrayList<TreePath>(selectedNodes.count())
        for (node in selectedNodes) {
            val wasExpanded = tree.isExpanded(TreePath(node.path))
            tree.removeNode(node)
            tree.insertNode(node, parent, index)
            if (parent.singleChoice) {
                tree.setNodeState(node, false)
            }
            val path = TreePath(node.path)
            newSelectionPaths.add(path)
            if (wasExpanded) {
                tree.expandPath(path)
            }
            index++
        }
        tree.selectionPaths = newSelectionPaths.toArray(arrayOf())
    }
}