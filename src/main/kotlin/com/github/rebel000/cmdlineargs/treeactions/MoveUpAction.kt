package com.github.rebel000.cmdlineargs.treeactions

import com.github.rebel000.cmdlineargs.ui.ArgumentTree
import com.github.rebel000.cmdlineargs.ui.ArgumentTreeNode
import com.intellij.ui.AnActionButton
import com.intellij.ui.AnActionButtonRunnable
import javax.swing.tree.TreePath
import kotlin.math.min

class MoveUpAction(private val tree: ArgumentTree) : AnActionButtonRunnable {
    override fun run(button: AnActionButton) {
        val selectedNodes = tree.selectedNodes(true)
        if (selectedNodes.isNotEmpty()) {
            val newSelectionPaths = ArrayList<TreePath>(selectedNodes.count())
            val anchorNode = selectedNodes.first()
            var parent = anchorNode.parent
            if (parent != null) {
                tree.lock()
                var index = parent.getIndex(anchorNode)
                if (index > 0) {
                    val neighbor = parent.getChildBefore(anchorNode)
                    if (neighbor?.isFolder == true) {
                        parent = neighbor
                        index = neighbor.childCount
                    }
                    else {
                        index--
                    }
                }
                else {
                    val grandparent = parent.parent ?: return
                    index = grandparent.getIndex(parent)
                    parent = grandparent
                }
                for (node in selectedNodes) {
                    val wasExpanded = tree.isExpanded(TreePath(node.path))
                    tree.removeNode(node)
                    tree.insertNode(node, parent, index)
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