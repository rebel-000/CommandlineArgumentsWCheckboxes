package com.github.rebel000.cmdlineargs.ui

import com.intellij.icons.AllIcons
import com.intellij.ui.CheckboxTree
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBRadioButton
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.ThreeStateCheckBox
import java.awt.BorderLayout
import java.awt.Color
import javax.swing.Icon
import javax.swing.JTree

class ArgumentTreeCellRenderer : CheckboxTree.CheckboxTreeCellRenderer(true, false) {
    private var shouldUseRadio: Boolean = false
    private val fgColor: Color = textRenderer.foreground
    val myRadioButton = JBRadioButton().apply { isOpaque = false }

    companion object {
        fun getIcon(node: ArgumentTreeNode) : Icon? {
            return if (node.isFolder)
                if (node.folderAsParameter)
                    if (node.singleChoice) AllIcons.Actions.GroupByModule
                    else AllIcons.Actions.GroupByModuleGroup
                else
                    if (node.singleChoice) AllIcons.Nodes.Module
                    else AllIcons.Nodes.Folder
            else null
        }

        val NOT_SUPPORTED_TEXT_ATTRIBUTES = SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, JBUI.CurrentTheme.IconBadge.WARNING)
    }

    override fun customizeRenderer(
        tree: JTree,
        value: Any,
        selected: Boolean,
        expanded: Boolean,
        leaf: Boolean,
        row: Int,
        hasFocus: Boolean
    ) {
        when (value) {
            is NotSupportedNode -> {
                myCheckbox.isVisible = false
                myRadioButton.isVisible = false
                textRenderer.icon = AllIcons.General.Warning
                textRenderer.appendTextPadding(10)
                textRenderer.append(value.toString(), NOT_SUPPORTED_TEXT_ATTRIBUTES)
            }

            is ArgumentTreeNode -> {
                val visible = value !is ArgumentTreeRootNode
                myCheckbox.isVisible = visible
                myRadioButton.isVisible = visible
                val parent = value.parent
                val singleChoice = parent is ArgumentTreeNode && parent.singleChoice
                if (shouldUseRadio != singleChoice) {
                    if (singleChoice) {
                        add(myRadioButton, BorderLayout.WEST)
                        remove(myCheckbox)
                    } else {
                        add(myCheckbox, BorderLayout.WEST)
                        remove(myRadioButton)
                    }
                    shouldUseRadio = singleChoice
                }
                myCheckbox.state = value.state
                myRadioButton.isSelected = value.state != ThreeStateCheckBox.State.NOT_SELECTED
                textRenderer.icon = getIcon(value)
                textRenderer.append("$value   ")
                val filters = value.filters.toString()
                if (filters.isNotEmpty()) {
                    textRenderer.append("$filters  ", SimpleTextAttributes.GRAYED_BOLD_ATTRIBUTES)
                }
                textRenderer.append(value.description, SimpleTextAttributes.GRAYED_ITALIC_ATTRIBUTES)
                textRenderer.foreground = fgColor
            }
        }
    }
}