package com.github.rebel000.cmdlineargs.treeactions

import com.github.rebel000.cmdlineargs.ArgumentsService
import com.github.rebel000.cmdlineargs.ui.ArgumentTree
import com.intellij.openapi.actionSystem.AnActionEvent

class RemoveAction : TreeActionBase() {
    override fun actionPerformed(e: AnActionEvent) {
        val tree = ArgumentTree.getInstance(e.project) ?: return
        if (tree.isEditing) return
        val index: Int? = tree.selectionRows?.firstOrNull()
        tree.removeNodes(tree.selectedNodes(false))
        if (index != null) {
            tree.addSelectionRow(index)
        }
    }

    override fun update(e: AnActionEvent) {
        super.update(e)
        val tree = ArgumentsService.getInstance(e.project ?: return).tree ?: return
        if (tree.isInlineEditorActive) {
            e.presentation.isEnabled = false
        }
    }
}