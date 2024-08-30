package com.github.rebel000.cmdlineargs.ui

import com.github.rebel000.cmdlineargs.Resources
import com.intellij.execution.RunManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.setEmptyState
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import com.jetbrains.rd.ui.bedsl.extensions.valueOrEmpty
import com.jetbrains.rider.projectView.solution
import java.awt.Component
import java.awt.Dimension
import javax.accessibility.AccessibleContext
import javax.accessibility.AccessibleState
import javax.swing.*

@Suppress("DialogTitleCapitalization")
class ArgumentPropertiesDialog(private val project: Project, private val node: ArgumentTreeNode) : DialogWrapper(true) {
    private val nameField: JBTextField = JBTextField(node.name)
    private val isFolderField: JBCheckBox = JBCheckBox(Resources.message("properties.isFolder"))
    private val isSingleChoiceField: JBCheckBox = JBCheckBox(Resources.message("properties.isSingleChoice"))
    private val platformFiltersField: JBTextField = JBTextField()
    private val configurationFiltersField: JBTextField = JBTextField()
    private val runConfigurationFiltersField: JBTextField = JBTextField()
    private val platformCheckboxes: MutableMap<String, JBCheckBox> = mutableMapOf()
    private val configCheckboxes: MutableMap<String, JBCheckBox> = mutableMapOf()
    private val runConfigCheckboxes: MutableMap<String, JBCheckBox> = mutableMapOf()
    private var lockFilters: Boolean = false

    init {
        title = Resources.message("properties.title")
        isFolderField.isSelected = node.isFolder
        isFolderField.addItemListener {
            val isSelected = isFolderField.isSelected
            isSingleChoiceField.isEnabled = isSelected
        }
        isSingleChoiceField.isSelected = node.singleChoice
        isSingleChoiceField.isEnabled = isFolderField.isSelected
        platformFiltersField.text = node.filters.platform
        platformFiltersField.setEmptyState(Resources.message("properties.platformFilters.desc"))
        platformFiltersField.accessibleContext.addPropertyChangeListener {
            if (it?.propertyName == AccessibleContext.ACCESSIBLE_STATE_PROPERTY
                    && it.oldValue == AccessibleState.FOCUSED
                    && it.newValue == null) {
                onFiltersFieldChanged(platformFiltersField, platformCheckboxes)
            }
        }
        configurationFiltersField.text = node.filters.configuration
        configurationFiltersField.setEmptyState(Resources.message("properties.configurationFilters.desc"))
        configurationFiltersField.accessibleContext.addPropertyChangeListener {
            if (it?.propertyName == AccessibleContext.ACCESSIBLE_STATE_PROPERTY
                    && it.oldValue == AccessibleState.FOCUSED
                    && it.newValue == null) {
                onFiltersFieldChanged(configurationFiltersField, configCheckboxes)
            }
        }
        runConfigurationFiltersField.text = node.filters.runConfiguration
        runConfigurationFiltersField.setEmptyState(Resources.message("properties.runConfigurationFilters.desc"))
        runConfigurationFiltersField.accessibleContext.addPropertyChangeListener {
            if (it?.propertyName == AccessibleContext.ACCESSIBLE_STATE_PROPERTY 
                    && it.oldValue == AccessibleState.FOCUSED 
                    && it.newValue == null) {
                onFiltersFieldChanged(runConfigurationFiltersField, runConfigCheckboxes)
            }
        }

        init()
    }

    private fun onFiltersFieldChanged(field: JBTextField, checkboxes: Map<String, JBCheckBox>) {
        if (!lockFilters) {
            lockFilters = true
            val filters = field.text
                    .split(';')
                    .filter(String::isNotEmpty)
            for ((name, checkbox) in checkboxes) {
                checkbox.isSelected = filters.contains(name)
            }
            lockFilters = false
        }
    }

    private fun patchFiltersField(field: JBTextField, cfgName: String, checked: Boolean) {
        if (!lockFilters) {
            lockFilters = true
            if (!checked) {
                field.text = field.text
                        .split(';')
                        .filter { it.trim() != cfgName }
                        .joinToString(";")
            } else {
                if (field.text.isEmpty()) {
                    field.text = cfgName
                }
                else {
                    field.text = field.text.trim(';') + ";$cfgName"
                }
            }
            lockFilters = false
        }
    }

    private fun parseFilters(str: String): List<String> {
        return str.split(';').filter(String::isNotEmpty)
    }
    
    private fun createFilterCheckboxes(field: JBTextField, items: List<String>, outCheckBoxes: MutableMap<String, JBCheckBox>): JBScrollPane {
        val panel = JPanel()
        panel.alignmentY = Component.TOP_ALIGNMENT
        panel.minimumSize = Dimension(Int.MAX_VALUE, 0)
        panel.maximumSize = Dimension(Int.MAX_VALUE, Int.MAX_VALUE)
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        val filters = parseFilters(field.text)
        items.distinct().forEach { item ->
            val checkbox = JBCheckBox(item)
            checkbox.isSelected = filters.contains(item)
            checkbox.addItemListener {
                patchFiltersField(field, item, checkbox.isSelected)
            }
            outCheckBoxes[item] = checkbox
            panel.add(checkbox)
        }

        return JBScrollPane(panel)
    }

    override fun createCenterPanel(): JComponent? {
        val builder = FormBuilder
            .createFormBuilder()
            .addLabeledComponent(Resources.message("properties.name"), nameField)
            .addLabeledComponent("", isFolderField)
            .addLabeledComponent("", isSingleChoiceField)
            .addComponent(JLabel(Resources.message("properties.filters")))
            .addComponent(JLabel(Resources.message("properties.filters.desc")))
            .addLabeledComponent(Resources.message("properties.platformFilters"), platformFiltersField)
            .addLabeledComponent(Resources.message("properties.configurationFilters"), configurationFiltersField)
            .addLabeledComponent(Resources.message("properties.runConfigurationFilters"), runConfigurationFiltersField)
            .addComponent(JLabel(Resources.message("properties.filters.desc")))
            .addComponent(JLabel(""))

        val configsPlatforms = project.solution.solutionProperties.configurationsAndPlatformsCollection.valueOrEmpty()
        val runConfigs = RunManager.getInstance(project).allConfigurationsList.map { it.name }
        val checkboxesPanel = JPanel()
        checkboxesPanel.add(createFilterCheckboxes(platformFiltersField, configsPlatforms.map { it.platform }, platformCheckboxes))
        checkboxesPanel.add(createFilterCheckboxes(configurationFiltersField, configsPlatforms.map { it.configuration }, configCheckboxes))
        checkboxesPanel.add(createFilterCheckboxes(runConfigurationFiltersField, runConfigs, runConfigCheckboxes))
        checkboxesPanel.layout = BoxLayout(checkboxesPanel, BoxLayout.X_AXIS)
        builder.addComponentFillVertically(checkboxesPanel, 5)
        return builder.panel
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