package com.github.rebel000.cmdlineargs

import com.intellij.icons.AllIcons
import com.intellij.ui.CheckboxTree
import java.awt.Dimension
import javax.swing.JTree

class ArgumentTreeCellRenderer: CheckboxTree.CheckboxTreeCellRenderer() {
    override fun customizeRenderer(tree: JTree,
                                   value: Any,
                                   selected: Boolean,
                                   expanded: Boolean,
                                   leaf: Boolean,
                                   row: Int,
                                   hasFocus: Boolean) {
        textRenderer.minimumSize = Dimension(200, 0);
        if (value is ArgumentTreeNode) {
            if (value.isFolder) {
                textRenderer.icon = AllIcons.Nodes.Folder
                textRenderer.append(String.format("[%s]", value.name))
            }
            else {
                textRenderer.append(value.name)
            }
        }
    }
}