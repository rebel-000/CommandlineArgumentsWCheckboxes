package com.github.rebel000.cmdlineargs

import com.intellij.openapi.project.Project
import com.intellij.ui.AnActionButton
import com.intellij.ui.AnActionButtonRunnable

class ArgumentActionMoveUp(private val tree: ArgumentTree, private val project: Project): AnActionButtonRunnable {
    override fun run(button: AnActionButton) {
        tree.moveNodesUp()
    }
}