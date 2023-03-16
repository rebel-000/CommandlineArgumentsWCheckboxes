package com.github.rebel000.cmdlineargs.treeactions

import com.github.rebel000.cmdlineargs.Resources
import com.github.rebel000.cmdlineargs.ui.ArgumentTree
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.ide.CopyPasteManager
import java.awt.datatransfer.StringSelection
import java.util.*

@Suppress("DialogTitleCapitalization")
class CopyAction(private val tree: ArgumentTree) :
    AnAction(Resources.message("action.copy"), Resources.message("action.copy"), AllIcons.Actions.Copy) {
    init {
        registerCustomShortcutSet(ActionManager.getInstance().getAction(IdeActions.ACTION_COPY).shortcutSet, tree)
    }

    override fun actionPerformed(e: AnActionEvent) {
        val nodes = tree.selectedNodes()
        val strings = Vector<String>()
        for (node in nodes) {
            node.toStrings(strings, 0)
        }
        CopyPasteManager.getInstance().setContents(StringSelection(strings.joinToString("\n")))
    }
}