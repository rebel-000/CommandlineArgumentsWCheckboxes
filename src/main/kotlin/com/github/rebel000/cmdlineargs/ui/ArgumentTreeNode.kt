package com.github.rebel000.cmdlineargs.ui

import com.github.rebel000.cmdlineargs.ArgumentFilter
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.intellij.util.ui.ThreeStateCheckBox
import java.util.*
import javax.swing.tree.MutableTreeNode
import javax.swing.tree.TreeNode

open class ArgumentTreeNode(var name: String, var isFolder: Boolean) : com.intellij.ui.CheckedTreeNode() {
    private var privateState: ThreeStateCheckBox.State = ThreeStateCheckBox.State.SELECTED
    var singleChoice: Boolean = false
    var filters: Filters = Filters()
    var isExpanded: Boolean = false

    val state: ThreeStateCheckBox.State
        get() = privateState

    fun findAncestorIn(nodes: Array<ArgumentTreeNode>): ArgumentTreeNode? {
        var node = getParent()
        while (node != null && !nodes.contains(node)) {
            node = node.getParent()
        }
        return node
    }

    fun checkIsAncestorIn(nodes: Array<ArgumentTreeNode>): Boolean {
        return findAncestorIn(nodes) != null
    }

    fun toStrings(out: Vector<String>, indent: Int) {
        out.ensureCapacity(out.size + childCount + 1)
        out.add("\t".repeat(indent) + name)
        for (child in childrenArgs()) {
            child.toStrings(out, indent + 1)
        }
    }

    open fun getArgs(out: Vector<String>, filter: ArgumentFilter) {
        if (filter.check(this)) {
            out.ensureCapacity(out.size + childCount + 1)
            if (isFolder) {
                for (child in childrenArgs()) {
                    child.getArgs(out, filter)
                }
            } else {
                out.add(name)
            }
        }
    }

    open fun toJson(): JsonObject {
        val result = JsonObject()
        result.addProperty("name", name)
        result.addProperty("checked", isChecked)
        if (isFolder) {
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
            for (child in childrenArgs()) {
                items.add(child.toJson())
            }
        }
        return result
    }

    open fun fromJson(json: JsonObject): ArgumentTreeNode? {
        val nameProperty = json.get("name")
        if (nameProperty != null) {
            name = nameProperty.asString
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
                        val childNode = ArgumentTreeNode("", false).fromJson(item.asJsonObject)
                        if (childNode != null) {
                            childNode.setParent(this)
                            children.insertElementAt(childNode, childCount)
                        }
                    }
                }
            }
            update()
            return this
        }
        return null
    }

    override fun getChildAt(index: Int): ArgumentTreeNode? {
        if (children == null) {
            throw ArrayIndexOutOfBoundsException("node has no children")
        }
        return children.elementAt(index) as ArgumentTreeNode?
    }

    override fun getChildAfter(node: TreeNode): ArgumentTreeNode? {
        return super.getChildAfter(node) as ArgumentTreeNode?
    }

    override fun getChildBefore(node: TreeNode): ArgumentTreeNode? {
        return super.getChildBefore(node) as ArgumentTreeNode?
    }

    fun childrenArgs(): Enumeration<ArgumentTreeNode> {
        @Suppress("UNCHECKED_CAST")
        return super.children() as Enumeration<ArgumentTreeNode>
    }

    private fun updateSingleChoice(checkedNode: ArgumentTreeNode) {
        if (singleChoice) {
            for (child in childrenArgs()) {
                if (child != checkedNode) {
                    child.uncheck()
                }
            }
        }
    }

    private fun update() {
        var newState: ThreeStateCheckBox.State = ThreeStateCheckBox.State.NOT_SELECTED
        if (singleChoice) {
            for (child in childrenArgs()) {
                if (child.isChecked) {
                    newState = child.state
                    break
                }
            }
        } else if (isFolder && childCount > 0) {
            var result: ThreeStateCheckBox.State? = null
            for (child in childrenArgs()) {
                val childStatus = child.state
                if (childStatus == ThreeStateCheckBox.State.DONT_CARE) {
                    result = ThreeStateCheckBox.State.DONT_CARE
                    break
                }
                if (result == null) {
                    result = childStatus
                } else if (result != childStatus) {
                    result = ThreeStateCheckBox.State.DONT_CARE
                    break
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
            p?.updateSingleChoice(this)
            p?.update()
        }
    }

    private fun check() {
        if (isFolder && childCount > 0) {
            if (singleChoice) {
                if (privateState == ThreeStateCheckBox.State.NOT_SELECTED) {
                    val child = getChildAt(0)!!
                    child.check()
                    privateState = child.state
                } else {
                    for (child in childrenArgs()) {
                        if (child.isChecked) {
                            child.check()
                            privateState = child.state
                            break
                        }
                    }
                }
            } else {
                var result: ThreeStateCheckBox.State? = null
                for (child in childrenArgs()) {
                    child.check()
                    val childStatus = child.state
                    if (childStatus == ThreeStateCheckBox.State.DONT_CARE) {
                        result = ThreeStateCheckBox.State.DONT_CARE
                        break
                    }
                    if (result == null) {
                        result = childStatus
                    } else if (result != childStatus) {
                        result = ThreeStateCheckBox.State.DONT_CARE
                        break
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
            for (child in childrenArgs()) {
                child.uncheck()
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
            p?.updateSingleChoice(this)
            p?.update()
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

    override fun getParent(): ArgumentTreeNode? {
        return super.getParent() as ArgumentTreeNode?
    }

    override fun setParent(newParent: MutableTreeNode?) {
        getParent()?.update()
        super.setParent(newParent)
    }

    override fun toString(): String {
        return name
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