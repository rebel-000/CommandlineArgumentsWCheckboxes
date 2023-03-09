package com.github.rebel000.cmdlineargs

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import javax.swing.JComponent
import javax.swing.JLabel

@Suppress("DialogTitleCapitalization")
class ArgumentNodePropertiesDialog(private val node: ArgumentTreeNode) : DialogWrapper(true) {
    private val nameField: JBTextField = JBTextField(node.name)
    private val folderField: JBCheckBox = JBCheckBox(Resources.message("properties.isFolder")).apply {
        isSelected = node.isFolder
    }

    init {
        title = Resources.message("properties.title")
        isResizable = false
        init()
    }

    override fun createCenterPanel(): JComponent? {
        this.setSize(350, 0)
        return FormBuilder
                .createFormBuilder()
                .addComponent(JLabel(Resources.message("properties.name")))
                .addComponent(nameField)
                .addComponent(folderField)
                .panel
    }

    override fun getPreferredFocusedComponent(): JComponent? {
        return nameField
    }

    override fun doOKAction() {
        node.name = nameField.text
        node.isFolder = folderField.isSelected
        super.doOKAction()
    }
}