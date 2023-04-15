package com.github.rebel000.cmdlineargs.treeactions

import com.github.rebel000.cmdlineargs.ui.ArgumentTree
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ide.CopyPasteManager
import java.awt.datatransfer.StringSelection
import java.util.*

class CopyAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val tree = ArgumentTree.getInstance(e.project) ?: return
        val nodes = tree.selectedNodes(false)
        val strings = Vector<String>()
        for (node in nodes) {
            if (!node.checkIsAncestorIn(nodes)) {
                node.toStrings(strings, 0)
            }
        }
        CopyPasteManager.getInstance().setContents(StringSelection(strings.joinToString("\n")))
    }
}