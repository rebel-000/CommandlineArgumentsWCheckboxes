package com.github.rebel000.cmdlineargs.treeactions

import com.github.rebel000.cmdlineargs.ui.ArgumentTree
import com.intellij.openapi.actionSystem.AnActionEvent
import icons.com.github.rebel000.cmdlineargs.treeactions.TreeActionBase

class ReloadAction : TreeActionBase() {
    override fun actionPerformed(e: AnActionEvent) {
        val tree = ArgumentTree.getInstance(e.project) ?: return
        tree.reloadState()
    }
}