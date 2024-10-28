package com.github.rebel000.cmdlineargs.treeactions

import com.github.rebel000.cmdlineargs.ArgumentsService
import com.github.rebel000.cmdlineargs.Resources
import com.intellij.execution.RunManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.ide.CopyPasteManager
import java.awt.datatransfer.StringSelection

@Suppress("DialogTitleCapitalization")
class CopyCmdAction : DefaultActionGroup(Resources.message("action.cmdlineargs.copycmd.text"), true) {
    override fun getChildren(e: AnActionEvent?): Array<AnAction> {
        return ArgumentsService.getInstance(e?.project ?: return emptyArray()).copyCmdArgsActions
    }
}