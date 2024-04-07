package com.github.rebel000.cmdlineargs.treeactions

import com.github.rebel000.cmdlineargs.ArgumentsService
import com.intellij.openapi.actionSystem.AnActionEvent

class ReloadAction : TreeActionBase() {
    override fun actionPerformed(e: AnActionEvent) {
        ArgumentsService.getInstance(e.project ?: return).reloadState()
    }
}