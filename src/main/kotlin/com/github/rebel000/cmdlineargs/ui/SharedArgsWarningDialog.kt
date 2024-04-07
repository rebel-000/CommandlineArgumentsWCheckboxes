package com.github.rebel000.cmdlineargs.ui

import com.github.rebel000.cmdlineargs.ArgumentsService
import com.github.rebel000.cmdlineargs.Resources
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBCheckBox
import com.intellij.util.ui.FormBuilder
import java.awt.Component
import javax.swing.*

class SharedArgsWarningDialog(private val argsService: ArgumentsService) : DialogWrapper(true) {
    private val warningCheck = JBCheckBox(Resources.message("toolwindow.showShared.check"))

    init {
        title = Resources.message("toolwindow.showShared.title")
        isResizable = false

        warningCheck.addItemListener {
            myOKAction.isEnabled = warningCheck.isSelected
        }
        myOKAction.isEnabled = false
        warningCheck.alignmentX = Component.RIGHT_ALIGNMENT

        init()
    }
    override fun createCenterPanel(): JComponent? {
        setSize(310, 0)
        return FormBuilder
            .createFormBuilder()
            .addComponent(JLabel(Resources.message("toolwindow.showShared.message")).apply {
                foreground = JBColor.RED
            })
            .addComponent(JLabel(""))
            .addLabeledComponent(JLabel(""), warningCheck)
            .panel
    }

    override fun doOKAction() {
        argsService.showSharedArgs = true
        super.doOKAction()
    }
}