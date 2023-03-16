package com.github.rebel000.cmdlineargs.ui

import com.github.rebel000.cmdlineargs.Resources
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.setEmptyState
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import javax.swing.JComponent
import javax.swing.JLabel

@Suppress("DialogTitleCapitalization")
class ArgumentPropertiesDialog(private val node: ArgumentTreeNode) : DialogWrapper(true) {
    private val nameField: JBTextField = JBTextField(node.name)
    private val isFolderField: JBCheckBox = JBCheckBox(Resources.message("properties.isFolder"))
    private val isSingleChoiceField: JBCheckBox = JBCheckBox(Resources.message("properties.isSingleChoice"))
    private val platformFiltersField: JBTextField = JBTextField()
    private val configurationFiltersField: JBTextField = JBTextField()
    private val runConfigurationFiltersField: JBTextField = JBTextField()

    init {
        title = Resources.message("properties.title")
        isResizable = false
        isFolderField.isSelected = node.isFolder
        isFolderField.addChangeListener { _ ->
            val isSelected = isFolderField.isSelected
            isSingleChoiceField.isEnabled = isSelected
        }
        isSingleChoiceField.isSelected = node.singleChoice
        isSingleChoiceField.isEnabled = isFolderField.isSelected
        platformFiltersField.text = node.filters.platform
        platformFiltersField.setEmptyState(Resources.message("properties.platformFilters.desc"))
        configurationFiltersField.text = node.filters.configuration
        configurationFiltersField.setEmptyState(Resources.message("properties.configurationFilters.desc"))
        runConfigurationFiltersField.text = node.filters.runConfiguration
        runConfigurationFiltersField.setEmptyState(Resources.message("properties.runConfigurationFilters.desc"))
        init()
    }

    override fun createCenterPanel(): JComponent? {
        setSize(350, 0)
        return FormBuilder
            .createFormBuilder()
            .addLabeledComponent(Resources.message("properties.name"), nameField)
            .addLabeledComponent("", isFolderField)
            .addLabeledComponent("", isSingleChoiceField)
            .addComponent(JLabel(Resources.message("properties.filters")))
            .addComponent(JLabel(Resources.message("properties.filters.desc")))
            .addLabeledComponent(Resources.message("properties.platformFilters"), platformFiltersField)
            .addLabeledComponent(Resources.message("properties.configurationFilters"), configurationFiltersField)
            .addLabeledComponent(Resources.message("properties.runConfigurationFilters"), runConfigurationFiltersField)
            .panel
    }

    override fun getPreferredFocusedComponent(): JComponent {
        return nameField
    }

    override fun doOKAction() {
        node.name = nameField.text
        node.isFolder = isFolderField.isSelected
        node.singleChoice = isSingleChoiceField.isSelected
        node.filters.platform = platformFiltersField.text
        node.filters.configuration = configurationFiltersField.text
        node.filters.runConfiguration = runConfigurationFiltersField.text
        super.doOKAction()
    }
}