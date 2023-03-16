package com.github.rebel000.cmdlineargs.treeactions

import com.github.rebel000.cmdlineargs.Resources
import com.github.rebel000.cmdlineargs.ui.ArgumentTree
import com.github.rebel000.cmdlineargs.ui.ArgumentTreeNode
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.ide.CopyPasteManager
import java.awt.datatransfer.DataFlavor
import kotlin.math.max

@Suppress("DialogTitleCapitalization")
class PasteAction(private val tree: ArgumentTree) :
    AnAction(Resources.message("action.paste"), Resources.message("action.paste"), AllIcons.Actions.MenuPaste) {
    init {
        registerCustomShortcutSet(ActionManager.getInstance().getAction(IdeActions.ACTION_PASTE).shortcutSet, tree)
    }

    override fun actionPerformed(e: AnActionEvent) {
        val str = CopyPasteManager.getInstance().getContents<String?>(DataFlavor.stringFlavor)
        if (str != null) {
            val args = str.split("\n")
            val indentSize = getIndentSize(args)
            if (args.isNotEmpty()) {
                tree.lock()
                val (baseParent, baseIndex) = tree.getInsertPosition(tree.selectedNode())
                val baseIndent = getIndent(args[0], indentSize)
                var index = baseIndex
                var depth = 0
                var indent = baseIndent
                var parent = baseParent
                var node: ArgumentTreeNode? = null
                for (arg in args) {
                    val currentIndent = max(getIndent(arg, indentSize), baseIndent)
                    if (currentIndent != indent) {
                        if (currentIndent > indent) {
                            if (node != null) {
                                node.isFolder = true
                                parent = node
                                indent = currentIndent
                                depth++
                                index = 0
                                tree.expandNode(parent, false)
                            }
                        } else {
                            while (currentIndent < indent) {
                                index = parent.parent!!.getIndex(parent) + 1
                                parent = parent.parent!!
                                indent--
                                depth--
                            }
                        }
                    }

                    node = ArgumentTreeNode(arg.trimStart(), false)
                    tree.insertNode(node, parent, index++)
                }
                tree.expandNode(baseParent, false)
                tree.unlock()
            }
        }
    }

    private fun getIndentSize(args: List<String>): Int {
        for (arg in args) {
            if (arg.startsWith(' ')) {
                var i = 0
                while (i < arg.length && arg[i] == ' ') i++
                return i
            } else if (arg.startsWith('\t')) {
                return 1
            }
        }

        return 1
    }

    private fun getIndent(str: String, indentSize: Int): Int {
        var i = 0
        if (str[0] == ' ') {
            while (i < str.length && str[i] == ' ') i++
        } else {
            while (i < str.length && str[i] == '\t') i++
        }
        return i / indentSize
    }
}