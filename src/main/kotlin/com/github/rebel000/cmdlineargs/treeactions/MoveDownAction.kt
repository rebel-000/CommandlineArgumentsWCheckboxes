package com.github.rebel000.cmdlineargs.treeactions

import com.github.rebel000.cmdlineargs.ui.ArgumentTree
import com.github.rebel000.cmdlineargs.ui.ArgumentTreeNode
import com.intellij.openapi.actionSystem.AnActionEvent
import javax.swing.tree.TreePath
import kotlin.math.min

class MoveDownAction : TreeActionBase() {
    override fun actionPerformed(e: AnActionEvent) {
        val tree = ArgumentTree.getInstance(e.project) ?: return
        var selectedNodes = tree.selectedNodes(true)
        if (selectedNodes.isEmpty()) {
            return
        }
        selectedNodes = selectedNodes.filter { !it.checkIsAncestorIn(selectedNodes) && !it.readonly }
        var anchorIdx = selectedNodes.count() - 1
        while (selectedNodes[anchorIdx].checkIsAncestorIn(selectedNodes)) {
            anchorIdx--
        }
        var anchorNode = selectedNodes[anchorIdx]
        var parent = anchorNode.parent
        if (parent !is ArgumentTreeNode) {
            return
        }
        var index = parent.getIndex(anchorNode) + 1
        if (index == 0) {
            return
        }
        val last = index == parent.childCount
        if (last) {
            anchorNode = parent
            parent = parent.parent ?: return
        }
        if (parent !is ArgumentTreeNode) {
            return
        }
        if (!last || anchorNode.readonly) {
            val sibling = parent.getChildAfter(anchorNode) as ArgumentTreeNode?
            if (sibling?.isFolder == true) {
                parent = sibling
                index = -1
            } else if (anchorNode.readonly) {
                return
            }
        }
        else {
            index = parent.getIndex(anchorNode)
        }
        ++index
        index = min(index, parent.childCount)
        val newSelectionPaths = ArrayList<TreePath>(selectedNodes.count())
        for (node in selectedNodes) {
            val wasExpanded = tree.isExpanded(TreePath(node.path))
            if (node.parent === parent && parent.getIndex(node) < index) {
                index--
            }
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