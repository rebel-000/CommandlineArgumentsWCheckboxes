package com.github.rebel000.cmdlineargs.ui

import java.awt.Component
import java.awt.Dimension
import javax.swing.DefaultCellEditor
import javax.swing.JComponent
import javax.swing.JTextField
import javax.swing.JTree
import javax.swing.event.CellEditorListener
import javax.swing.event.ChangeEvent
import javax.swing.tree.TreePath
import kotlin.math.max

class ArgumentTreeCellEditor(private val tree: ArgumentTree) : DefaultCellEditor(JTextField()) {
    private val renderer: ArgumentTreeCellRenderer get() = tree.cellRenderer as ArgumentTreeCellRenderer
    private val textField = editorComponent as JTextField
    private var currentNode: ArgumentTreeNode? = null
    private var offset: Int = 0
    private val baseOffset: Int = renderer.insets.left + renderer.textRenderer.ipad.left + renderer.textRenderer.myBorder.getBorderInsets(renderer.textRenderer).left - textField.insets.left - textField.margin.left
    private val folderOffset: Int = textField.getFontMetrics(textField.font).charWidth('[')

    private val myEditorComponent = object : JComponent() {
        var myY: Int? = null
        var myHeight: Int? = null

        override fun setBounds(x: Int, y: Int, width: Int, height: Int) {
            super.setBounds(x, myY ?: y, width, myHeight ?: height)
        }

        override fun doLayout() {
            if (componentOrientation.isLeftToRight) {
                textField.setBounds(offset, 0, width - offset, height)
            } else {
                textField.setBounds(0, 0, width - offset, height)
            }
        }

        override fun getPreferredSize(): Dimension {
            val pSize = textField.preferredSize
            pSize.width = max((pSize.width + offset + 5).toDouble(), tree.width * 0.7).toInt()
            return pSize
        }
    }

    init {
        addCellEditorListener(object : CellEditorListener {
            override fun editingStopped(e: ChangeEvent?) {
                val node = currentNode ?: return
                val value = cellEditorValue as String
                if (value.isNotEmpty()) {
                    node.name = value
                }
                else {
                    val select = node.nextSibling ?: node.previousSibling ?: node.parent
                    tree.removeNode(node)
                    if (select != null) {
                        tree.selectionPaths = arrayOf(TreePath(node.path))
                    }
                }
            }

            override fun editingCanceled(e: ChangeEvent?) {
                editingStopped(e)
            }
        })
        editorComponent = myEditorComponent
        editorComponent.add(textField)
    }

    override fun getTreeCellEditorComponent(tree: JTree, value: Any, isSelected: Boolean, expanded: Boolean, leaf: Boolean, row: Int): Component {
        if (value is ArgumentTreeNode) {
            currentNode = value
            val icon = ArgumentTreeCellRenderer.getIcon(value)
            offset = if (renderer.myCheckbox.parent != null) renderer.myCheckbox.width else renderer.myRadioButton.width
            offset += baseOffset
            if (icon != null) {
                offset += folderOffset + icon.iconWidth + renderer.textRenderer.iconTextGap
            }
            delegate.setValue(value.name)
        }
        else {
            offset = 0
            delegate.setValue(tree.convertValueToText(value, isSelected, expanded, leaf, row, false))
        }
        val b = tree.getRowBounds(row)
        myEditorComponent.myY = b.y - 4
        myEditorComponent.myHeight = b.height + 8
        return editorComponent
    }
}