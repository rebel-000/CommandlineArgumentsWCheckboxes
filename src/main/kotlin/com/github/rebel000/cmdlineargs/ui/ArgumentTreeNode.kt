package com.github.rebel000.cmdlineargs.ui

import com.github.rebel000.cmdlineargs.ArgumentFilter
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.intellij.util.ui.ThreeStateCheckBox
import java.util.*
import javax.swing.tree.MutableTreeNode

open class ArgumentTreeNode(var name: String, var isFolder: Boolean, val readonly: Boolean) : com.intellij.ui.CheckedTreeNode() {
    private var privateState: ThreeStateCheckBox.State = ThreeStateCheckBox.State.SELECTED
    var singleChoice: Boolean = false
    var filters: Filters = Filters()
    var isExpanded: Boolean = true
    var folderAsParameter: Boolean = false
    var joinChildren: Boolean = false
    var joinDelimiter: String = ","
    var joinPrefix: String = ""
    var joinPostfix: String = ""

    open val state: ThreeStateCheckBox.State
        get() = privateState

    fun checkIsAncestorIn(nodes: List<ArgumentTreeNode>): Boolean {
        var node = parent
        while (node is ArgumentTreeNode && !nodes.contains(node)) {
            node = node.parent
        }
        return node != null
    }

    fun toStrings(out: Vector<String>, indent: Int) {
        if (!readonly && (!isFolder || childCount > 0)) {
            out.ensureCapacity(out.size + childCount + 1)
            out.add("\t".repeat(indent) + name)
        }
        forEachArg {
            it.toStrings(out, indent + 1)
        }
    }

    open fun getArgs(out: Vector<String>, filter: ArgumentFilter) {
        if (filter.check(this)) {
            if (isFolder) {
                if (joinChildren) {
                    val value = Vector<String>()
                    forEachArg {
                        it.getArgs(value, filter)
                    }
                    if (folderAsParameter) {
                        out.add("${name}${value.joinToString(joinDelimiter, joinPrefix, joinPostfix)}")
                    }
                    else {
                        out.add(value.joinToString(joinDelimiter, joinPrefix, joinPostfix))
                    }
                }
                else {
                    out.ensureCapacity(out.size + childCount + 1)
                    if (folderAsParameter) {
                        out.add(name)
                    }
                    forEachArg {
                        it.getArgs(out, filter)
                    }
                }
            }
            else {
                out.add(name)
            }
        }
    }

    open fun toJson(): JsonObject {
        val result = JsonObject()
        result.addProperty("name", name)
        result.addProperty("checked", isChecked)
        if (isFolder) {
            result.addProperty("param", folderAsParameter)
            if (joinChildren) {
                result.addProperty("join", true)
                result.addProperty("join.delimiter", joinDelimiter)
                result.addProperty("join.prefix", joinPrefix)
                result.addProperty("join.postfix", joinPostfix)
            }
            result.addProperty("expanded", isExpanded)
            result.addProperty("singleChoice", singleChoice)
        }
        if (filters.platform.isNotEmpty() || filters.configuration.isNotEmpty() || filters.runConfiguration.isNotEmpty()) {
            val jFilters = JsonObject()
            jFilters.addProperty("platform", filters.platform)
            jFilters.addProperty("configuration", filters.configuration)
            jFilters.addProperty("runConfiguration", filters.runConfiguration)
            result.add("filters", jFilters)
        }
        if (isFolder) {
            val items = JsonArray(childCount)
            result.add("items", items)
            forEachArg {
                items.add(it.toJson())
            }
        }
        return result
    }

    open fun fromJson(json: JsonObject): Boolean {
        val nameProperty = json.get("name")
        if (nameProperty != null) {
            name = nameProperty.asString
            folderAsParameter = json.get("param")?.asBoolean == true
            joinChildren = json.get("join")?.asBoolean == true
            joinDelimiter = json.get("join.delimiter")?.asString ?: ","
            joinPrefix = json.get("join.prefix")?.asString ?: ""
            joinPostfix = json.get("join.postfix")?.asString ?: ""
            setChecked(json.get("checked")?.asBoolean == true)
            isExpanded = json.get("expanded")?.asBoolean == true
            singleChoice = json.get("singleChoice")?.asBoolean == true
            val jFilters = json.get("filters")?.asJsonObject
            if (jFilters != null) {
                filters.platform = jFilters.get("platform")?.asString.orEmpty()
                filters.configuration = jFilters.get("configuration")?.asString.orEmpty()
                filters.runConfiguration = jFilters.get("runConfiguration")?.asString.orEmpty()
            }
            val items = json.getAsJsonArray("items")
            if (items != null) {
                isFolder = true
                children = Vector(items.size())
                for (item in items) {
                    if (item.isJsonObject) {
                        val childNode = ArgumentTreeNode("", false, readonly = false)
                        if (childNode.fromJson(item.asJsonObject)) {
                            childNode.setParent(this)
                            children.insertElementAt(childNode, childCount)
                        }
                    }
                }
            }
            update()
            return true
        }
        return false
    }
    
    fun forEachArg(f: (ArgumentTreeNode) -> Unit) {
        if (children != null) {
            for (child in children) {
                if (child is ArgumentTreeNode) {
                    f(child)
                }
            }
        }
    }

    private fun updateSingleChoice(checkedNode: ArgumentTreeNode) {
        if (singleChoice) {
            forEachArg {
                if (it != checkedNode) {
                    it.uncheck()
                }
            }
        }
    }

    protected open fun update() {
        var newState: ThreeStateCheckBox.State = ThreeStateCheckBox.State.NOT_SELECTED
        if (singleChoice) {
            for (child in children) {
                if (child is ArgumentTreeNode && child.isChecked) {
                    newState = child.state
                    break
                }
            }
        } else if (isFolder && childCount > 0) {
            var result: ThreeStateCheckBox.State? = null
            for (child in children) {
                if (child is ArgumentTreeNode) {
                    val childStatus = child.state
                    if (childStatus == ThreeStateCheckBox.State.DONT_CARE || childStatus != (result ?: childStatus)) {
                        result = ThreeStateCheckBox.State.DONT_CARE
                        break
                    }
                    result = childStatus
                }
            }
            newState = result ?: ThreeStateCheckBox.State.NOT_SELECTED
        } else if (isChecked) {
            newState = ThreeStateCheckBox.State.SELECTED
        }
        if (privateState != newState) {
            privateState = newState
            isChecked = privateState != ThreeStateCheckBox.State.NOT_SELECTED
            val p = getParent()
            if (p is ArgumentTreeNode) {
                p.updateSingleChoice(this)
                p.update()
            }
        }
    }

    private fun check() {
        if (isFolder && childCount > 0) {
            if (singleChoice) {
                if (privateState == ThreeStateCheckBox.State.NOT_SELECTED) {
                    val child = getChildAt(0) as ArgumentTreeNode
                    child.check()
                    privateState = child.state
                } else {
                    forEachArg {
                        if (it.isChecked) {
                            it.check()
                            privateState = it.state
                        }
                    }
                }
            } else {
                var result: ThreeStateCheckBox.State? = null
                for (child in children) {
                    if (child is ArgumentTreeNode) {
                        child.check()
                        val childStatus = child.state
                        if (childStatus == ThreeStateCheckBox.State.DONT_CARE || childStatus != (result
                                ?: childStatus)
                        ) {
                            result = ThreeStateCheckBox.State.DONT_CARE
                            break
                        }
                        result = childStatus
                    }
                }
                privateState = result ?: ThreeStateCheckBox.State.NOT_SELECTED
            }
        } else {
            privateState = ThreeStateCheckBox.State.SELECTED
        }
        isChecked = privateState != ThreeStateCheckBox.State.NOT_SELECTED
    }

    private fun uncheck() {
        if (childCount > 0) {
            forEachArg {
                it.uncheck()
            }
        }
        isChecked = false
        privateState = ThreeStateCheckBox.State.NOT_SELECTED
    }

    override fun setChecked(checked: Boolean) {
        if (isChecked != checked) {
            if (checked || state == ThreeStateCheckBox.State.DONT_CARE) {
                check()
            } else {
                uncheck()
            }
            val p = getParent()
            if (p is ArgumentTreeNode) {
                p.updateSingleChoice(this)
                p.update()
            }
        }
    }

    override fun insert(newChild: MutableTreeNode?, childIndex: Int) {
        super.insert(newChild, childIndex)
        if (newChild is ArgumentTreeNode) {
            if (!isChecked || singleChoice && newChild.isChecked) {
                newChild.uncheck()
            }
        }
        update()
    }

    override fun setParent(newParent: MutableTreeNode?) {
        (parent as? ArgumentTreeNode)?.update()
        super.setParent(newParent)
    }

    override fun toString(): String {
        return if (isFolder) "[${name}]" else name
    }

    class Filters {
        private var platformValue: String = ""
        private var configurationValue: String = ""
        private var runConfigurationValue: String = ""
        private var string: String? = null

        var platform: String
            get() = platformValue
            set(value) {
                platformValue = value
                string = null
            }
        var configuration: String
            get() = configurationValue
            set(value) {
                configurationValue = value
                string = null
            }
        var runConfiguration: String
            get() = runConfigurationValue
            set(value) {
                runConfigurationValue = value
                string = null
            }

        override fun toString(): String {
            if (string == null) {
                if (platform.isNotEmpty() || configuration.isNotEmpty() || runConfiguration.isNotEmpty()) {
                    val sb = StringBuilder()
                    if (platform.isNotEmpty()) {
                        sb.append("p=")
                        sb.append(platform)
                    }
                    if (configuration.isNotEmpty()) {
                        if (sb.isNotEmpty()) {
                            sb.append("|")
                        }
                        sb.append("c=")
                        sb.append(configuration)
                    }
                    if (runConfiguration.isNotEmpty()) {
                        if (sb.isNotEmpty()) {
                            sb.append("|")
                        }
                        sb.append("r=")
                        sb.append(runConfiguration)
                    }
                    string = String.format("(%s)", sb)
                } else {
                    string = ""
                }
            }
            return string!!
        }
    }
}