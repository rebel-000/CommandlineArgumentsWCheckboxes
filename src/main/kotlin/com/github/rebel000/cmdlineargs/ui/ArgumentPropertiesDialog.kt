package com.github.rebel000.cmdlineargs.ui

import com.github.rebel000.cmdlineargs.ArgumentFilter
import com.github.rebel000.cmdlineargs.ArgumentsService
import com.github.rebel000.cmdlineargs.Resources
import com.intellij.execution.RunManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.setEmptyState
import com.intellij.openapi.util.DimensionService
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import com.jetbrains.rd.ui.bedsl.extensions.valueOrEmpty
import com.jetbrains.rider.projectView.solution
import java.awt.Component
import java.awt.Dimension
import java.awt.Point
import java.util.*
import javax.accessibility.AccessibleContext
import javax.accessibility.AccessibleState
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

@Suppress("DialogTitleCapitalization")
class ArgumentPropertiesDialog(private val project: Project, private val node: ArgumentTreeNode) : DialogWrapper(true) {
    companion object {
        const val COMMANDLINEARGS_PROPERTIES_DIALOG_ID = "COMMANDLINEARGS_PROPERTIES_DIALOG_ID"
    }

    private val argsService = ArgumentsService.getInstance(project)
    private val nameField: JBTextField = JBTextField(node.name).apply {
        document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) { refreshPreview() }
            override fun removeUpdate(e: DocumentEvent?) { refreshPreview() }
            override fun changedUpdate(e: DocumentEvent?) { refreshPreview() }
        })
    }

    private val descField: JBTextField = JBTextField(node.description)

    private val previewField: JBTextField = JBTextField().apply {
        isEditable = false
        border = BorderFactory.createEmptyBorder()
        isOpaque = false
    }

    private val isFolderField: JBCheckBox = JBCheckBox(Resources.message("properties.isFolder")).apply {
        isSelected = node.isFolder
        addItemListener {
            refresh()
        }
    }

    private val folderAsParameterField: JBCheckBox = JBCheckBox(Resources.message("properties.folderAsParameter")).apply {
        isSelected = node.folderAsParameter
        addItemListener {
            refresh()
        }
    }

    private val joinChildrenField: JBCheckBox = JBCheckBox(Resources.message("properties.enable")).apply {
        isSelected = node.joinChildren
        addItemListener {
            refresh()
        }
    }
    
    private val joinDelimiterField: JBTextField = JBTextField(",").apply {
        text = node.joinDelimiter
        maximumSize = Dimension(50, Int.MAX_VALUE)
        document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) { refreshPreview() }
            override fun removeUpdate(e: DocumentEvent?) { refreshPreview() }
            override fun changedUpdate(e: DocumentEvent?) { refreshPreview() }
        })
    }

    private val joinPrefixField: JBTextField = JBTextField("\"").apply {
        text = node.joinPrefix
        maximumSize = Dimension(50, Int.MAX_VALUE)
        document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) { refreshPreview() }
            override fun removeUpdate(e: DocumentEvent?) { refreshPreview() }
            override fun changedUpdate(e: DocumentEvent?) { refreshPreview() }
        })
    }

    private val joinPostfixField: JBTextField = JBTextField("\"").apply {
        text = node.joinPostfix
        maximumSize = Dimension(50, Int.MAX_VALUE)
        document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) { refreshPreview() }
            override fun removeUpdate(e: DocumentEvent?) { refreshPreview() }
            override fun changedUpdate(e: DocumentEvent?) { refreshPreview() }
        })
    }

    private val isSingleChoiceField: JBCheckBox = JBCheckBox(Resources.message("properties.isSingleChoice")).apply {
        isSelected = node.singleChoice
        addItemListener {
            refreshPreview()
        }
    }

    private val platformFiltersField: JBTextField = JBTextField().apply {
        text = node.filters.platform
        setEmptyState(Resources.message("properties.platformFilters.desc"))
        accessibleContext.addPropertyChangeListener {
            if (it?.propertyName == AccessibleContext.ACCESSIBLE_STATE_PROPERTY
                && it.oldValue == AccessibleState.FOCUSED
                && it.newValue == null) {
                refreshFilters(this, platformCheckboxes)
            }
        }
    }

    private val configurationFiltersField: JBTextField = JBTextField().apply {
        text = node.filters.configuration
        setEmptyState(Resources.message("properties.configurationFilters.desc"))
        accessibleContext.addPropertyChangeListener {
            if (it?.propertyName == AccessibleContext.ACCESSIBLE_STATE_PROPERTY
                && it.oldValue == AccessibleState.FOCUSED
                && it.newValue == null) {
                refreshFilters(this, configCheckboxes)
            }
        }
    }

    private val runConfigurationFiltersField: JBTextField = JBTextField().apply {
        text = node.filters.runConfiguration
        setEmptyState(Resources.message("properties.runConfigurationFilters.desc"))
        accessibleContext.addPropertyChangeListener {
            if (it?.propertyName == AccessibleContext.ACCESSIBLE_STATE_PROPERTY
                && it.oldValue == AccessibleState.FOCUSED
                && it.newValue == null) {
                refreshFilters(this, runConfigCheckboxes)
            }
        }
    }

    private val platformCheckboxes: MutableMap<String, JBCheckBox> = mutableMapOf()
    private val configCheckboxes: MutableMap<String, JBCheckBox> = mutableMapOf()
    private val runConfigCheckboxes: MutableMap<String, JBCheckBox> = mutableMapOf()
    private val previewNode = ArgumentTreeNode(nameField.text, isFolderField.isSelected, false)
    private var previewFilter: ArgumentFilter? = null
    private var freeze = false

    init {
        title = Resources.message("properties.title")
        init()

        val dimensionService: DimensionService = DimensionService.getInstance()
        val size: Dimension? = dimensionService.getSize(COMMANDLINEARGS_PROPERTIES_DIALOG_ID, project)
        val location: Point? = dimensionService.getLocation(COMMANDLINEARGS_PROPERTIES_DIALOG_ID, project)

        if (size != null) {
            setSize(size.width, size.height)
        }

        if (location != null) {
            setLocation(location)
        }

        previewNode.add(ArgumentTreeNode("{value1}", false, false))
        previewNode.add(ArgumentTreeNode("{value2}", false, false))
        previewNode.add(ArgumentTreeNode("{value3}", false, false))
        previewNode.isChecked = false
        previewNode.isChecked = true

        refresh()
    }

    private fun refresh() {
        if (freeze) {
            return
        }
        freeze = true
        val isFolder = isFolderField.isSelected
        val isJoin = isFolder && joinChildrenField.isSelected
        folderAsParameterField.isEnabled = isFolder
        joinChildrenField.isEnabled = isFolder
        isSingleChoiceField.isEnabled = isFolder
        joinDelimiterField.isEnabled = isJoin
        joinPrefixField.isEnabled = isJoin
        joinPostfixField.isEnabled = isJoin
        freeze = false
        refreshPreview()
    }

    private fun refreshFilters(field: JBTextField, checkboxes: Map<String, JBCheckBox>) {
        if (freeze) {
            return
        }
        freeze = true
        val filters = field.text
                .split(';')
                .filter(String::isNotEmpty)
        for ((name, checkbox) in checkboxes) {
            checkbox.isSelected = filters.contains(name)
        }
        freeze = false
        refreshPreview()
    }

    private fun refreshPreview() {
        if (freeze) {
            return
        }
        freeze = true
        if (previewFilter == null) {
            val runConfiguration = RunManager.getInstance(project).selectedConfiguration?.name
            val configurationAndPlatform = project.solution.solutionProperties.activeConfigurationPlatform.value
            previewFilter = ArgumentFilter(configurationAndPlatform?.configuration, configurationAndPlatform?.platform, runConfiguration)
        }
        val args = Vector<String>()
        applyTo(previewNode)
        previewNode.isChecked = false
        previewNode.isChecked = true
        previewNode.getArgs(args, previewFilter!!)
        previewField.text = args.joinToString(" ")
        freeze = false
    }
    
    private fun createFilterCheckboxes(field: JBTextField, items: List<String>, outCheckBoxes: MutableMap<String, JBCheckBox>): JPanel {
        val panel = JPanel()
        panel.alignmentY = Component.TOP_ALIGNMENT
        panel.minimumSize = Dimension(Int.MAX_VALUE, 0)
        panel.maximumSize = Dimension(Int.MAX_VALUE, Int.MAX_VALUE)
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        val filters = field.text.split(';').filter(String::isNotEmpty)
        items.distinct().forEach { item ->
            val checkbox = JBCheckBox(item)
            checkbox.isSelected = filters.contains(item)
            checkbox.addItemListener {
                if (!freeze) {
                    freeze = true
                    val checked = checkbox.isSelected
                    if (!checked) {
                        field.text = field.text
                            .split(';')
                            .filter { it.trim() != item }
                            .joinToString(";")
                    } else {
                        if (field.text.isEmpty()) {
                            field.text = item
                        }
                        else {
                            field.text = field.text.trim(';') + ";$item"
                        }
                    }
                    freeze = false
                }
            }
            outCheckBoxes[item] = checkbox
            panel.add(checkbox)
        }

        panel.border = BorderFactory.createLineBorder(JBColor.border(), 1, true)
        return panel
    }

    override fun createCenterPanel(): JComponent {
        return JBScrollPane(FormBuilder
            .createFormBuilder()
            .addLabeledComponent(Resources.message("properties.name"), nameField)
            .addLabeledComponent(Resources.message("properties.desc"), descField)
            .addSeparator()
            .addComponentToRightColumn(isFolderField)
            .addComponentToRightColumn(folderAsParameterField)
            .addComponentToRightColumn(isSingleChoiceField)
            .addSeparator()
            .addLabeledComponent(Resources.message("properties.join"), joinChildrenField)
            .addLabeledComponent(Resources.message("properties.joinDelimiter"), joinDelimiterField)
            .addLabeledComponent(Resources.message("properties.joinPrefix"), joinPrefixField)
            .addLabeledComponent(Resources.message("properties.joinPostfix"), joinPostfixField)
            .addSeparator()
            .addLabeledComponent(Resources.message("properties.preview"), previewField)
            .addSeparator()
            .addComponent(JLabel(Resources.message("properties.filters")))
            .addComponent(JLabel(Resources.message("properties.filters.desc")))
            .addLabeledComponent(Resources.message("properties.platformFilters"), platformFiltersField)
            .addLabeledComponent(Resources.message("properties.configurationFilters"), configurationFiltersField)
            .addLabeledComponent(Resources.message("properties.runConfigurationFilters"), runConfigurationFiltersField)
            .addComponent(JLabel(Resources.message("properties.filters.desc")))
            .addSeparator()
            .addComponentFillVertically(JPanel().apply {
            val configsPlatforms = project.solution.solutionProperties.configurationsAndPlatformsCollection.valueOrEmpty()
            val runConfigs = RunManager.getInstance(project).allConfigurationsList.filter { argsService.isConfigSupported(it) }.map { it.name }
            add(createFilterCheckboxes(runConfigurationFiltersField, runConfigs, runConfigCheckboxes))
            add(createFilterCheckboxes(configurationFiltersField, configsPlatforms.map { it.configuration }, configCheckboxes))
            add(createFilterCheckboxes(platformFiltersField, configsPlatforms.map { it.platform }, platformCheckboxes))
            layout = BoxLayout(this, BoxLayout.X_AXIS)
        }, 5).panel.apply {
            border = BorderFactory.createEmptyBorder(0, 0, 0, 20)
        }).apply {
            preferredSize = JBUI.size(970, 680);
            border = null
        }
    }

    override fun getPreferredFocusedComponent(): JComponent {
        return nameField
    }

    override fun doOKAction() {
        applyTo(node)
        node.filters.platform = platformFiltersField.text
        node.filters.configuration = configurationFiltersField.text
        node.filters.runConfiguration = runConfigurationFiltersField.text
        saveDimensions()
        super.doOKAction()
    }

    private fun applyTo(node: ArgumentTreeNode) {
        node.name = nameField.text
        node.description = descField.text
        node.isFolder = isFolderField.isSelected
        node.folderAsParameter = folderAsParameterField.isSelected
        node.singleChoice = isSingleChoiceField.isSelected
        node.joinChildren = joinChildrenField.isSelected
        node.joinDelimiter = joinDelimiterField.text
        node.joinPrefix = joinPrefixField.text
        node.joinPostfix = joinPostfixField.text
    }

    override fun doCancelAction() {
        saveDimensions()
        super.doCancelAction()
    }

    private fun saveDimensions() {
        val dimensionService = DimensionService.getInstance()
        val size = size
        val location = location
        dimensionService.setSize(COMMANDLINEARGS_PROPERTIES_DIALOG_ID, size, project)
        dimensionService.setLocation(COMMANDLINEARGS_PROPERTIES_DIALOG_ID, location, project)
    }
}