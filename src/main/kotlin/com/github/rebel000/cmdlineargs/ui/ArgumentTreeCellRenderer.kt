package com.github.rebel000.cmdlineargs.ui

import com.intellij.icons.AllIcons
import com.intellij.ui.CheckboxTree
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBRadioButton
import com.intellij.util.ui.ThreeStateCheckBox
import java.awt.BorderLayout
import javax.swing.JTree

class ArgumentTreeCellRenderer : CheckboxTree.CheckboxTreeCellRenderer(true, false) {
    private val myRadioButton = JBRadioButton()
    private var shouldUseRadio = false

    init {
        myRadioButton.isOpaque = false
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
        val visible = value !is ArgumentTreeRootNode
        myCheckbox.isVisible = visible
        myRadioButton.isVisible = visible
        if (value is ArgumentTreeNode) {
            val singleChoice = value.parent?.singleChoice == true
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
            var padding = 5
            if (value.name.length < 100) {
                padding = 100
            }
            if (value.isFolder) {
                textRenderer.preferredSize
                textRenderer.icon = if (value.singleChoice) AllIcons.Nodes.Module else AllIcons.Nodes.Folder
                textRenderer.append(String.format("[%s]  ", value.name))
                textRenderer.append(value.filters.toString(), SimpleTextAttributes.GRAYED_ITALIC_ATTRIBUTES)
                textRenderer.appendTextPadding(padding)
            } else {
                textRenderer.append(value.name)
                textRenderer.append(value.filters.toString(), SimpleTextAttributes.GRAYED_ITALIC_ATTRIBUTES)
                textRenderer.appendTextPadding(padding)
            }
        }
    }
}