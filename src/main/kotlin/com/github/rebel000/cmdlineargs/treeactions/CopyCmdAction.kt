package com.github.rebel000.cmdlineargs.treeactions

import com.github.rebel000.cmdlineargs.ArgumentsService
import com.github.rebel000.cmdlineargs.Resources
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup

class CopyCmdAction : DefaultActionGroup(Resources.message("action.cmdlineargs.copycmd.text"), true) {
    override fun getChildren(e: AnActionEvent?): Array<AnAction> {
        return ArgumentsService.getInstance(e?.project ?: return emptyArray()).copyCmdArgsActions
    }
}