package com.github.rebel000.cmdlineargs.treeactions

import com.github.rebel000.cmdlineargs.ui.ArgumentTree
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import javax.swing.tree.TreePath
import kotlin.math.min

class MoveDownAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val tree = ArgumentTree.getInstance(e.project) ?: return
        val selectedNodes = tree.selectedNodes(true)
        if (selectedNodes.isNotEmpty()) {
            val newSelectionPaths = ArrayList<TreePath>(selectedNodes.count())
            var anchorIdx = selectedNodes.count() - 1
            while (selectedNodes[anchorIdx].checkIsAncestorIn(selectedNodes)) {
                anchorIdx--
            }
            val anchorNode = selectedNodes[anchorIdx]
            var parent = anchorNode.parent
            if (parent != null) {
                tree.lock()
                var index = parent.getIndex(anchorNode)
                if (index < parent.childCount - 1) {
                    val neighbor = parent.getChildAfter(anchorNode)
                    if (neighbor?.isFolder == true) {
                        parent = neighbor
                        index = 0
                    } else {
                        index += 2
                    }
                } else {
                    val grandparent = parent.parent ?: return
                    index = grandparent.getIndex(parent) + 1
                    parent = grandparent
                }
                index = min(index, parent.childCount)
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
                tree.unlock()
            }
        }
    }
}