package com.github.rebel000.cmdlineargs

import com.intellij.openapi.project.Project
import com.intellij.ui.AnActionButton
import com.intellij.ui.AnActionButtonRunnable

class ArgumentActionRemove(private val tree: ArgumentTree, private val project: Project): AnActionButtonRunnable {
    override fun run(button: AnActionButton) {
        val selectedNodes = tree.selectedNodes()
        var i = selectedNodes.size - 1
        while (i >= 0) {
            tree.removeNode(selectedNodes[i])
            i--
        }
    }
}