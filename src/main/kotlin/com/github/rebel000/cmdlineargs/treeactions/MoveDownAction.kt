package com.github.rebel000.cmdlineargs.treeactions

import com.github.rebel000.cmdlineargs.ui.ArgumentTree
import com.intellij.ui.AnActionButton
import com.intellij.ui.AnActionButtonRunnable

class MoveDownAction(private val tree: ArgumentTree) : AnActionButtonRunnable {
    override fun run(button: AnActionButton) {
        val selectedNodes = tree.selectedNodes()
        tree.lock()
        for (node in selectedNodes) {
            val parent = node.parent
            if (parent != null) {
                val index = parent.getIndex(node)
                var selIndex = tree.selectionRows[0]
                if (index < parent.childCount - 1) {
                    val neighbor = parent.getChildAfter(node)
                    if (neighbor?.isFolder == true) {
                        tree.removeNode(node)
                        tree.insertNode(node, neighbor, 0)
                    } else {
                        tree.removeNode(node)
                        tree.insertNode(node, parent, index + 1)
                        ++selIndex
                    }
                } else {
                    val grandparent = parent.parent
                    if (grandparent != null) {
                        val parentIndex = grandparent.getIndex(parent)
                        tree.removeNode(node)
                        tree.insertNode(node, grandparent, parentIndex + 1)
                    }
                }
                tree.expandNode(parent, false)
                tree.addSelectionRow(selIndex)
            }
        }
        tree.unlock()
    }
}