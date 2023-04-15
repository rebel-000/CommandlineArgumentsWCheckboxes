package com.github.rebel000.cmdlineargs.ui

import com.github.rebel000.cmdlineargs.ArgumentFilter
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import java.util.*
import javax.swing.tree.TreeNode

open class ArgumentTreeNode(var name: String, var isFolder: Boolean) : com.intellij.ui.CheckedTreeNode() {
    var singleChoice: Boolean = false
    var filters: Filters = Filters()
    var isExpanded: Boolean = false

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
                for (item in items) {
                    if (item.isJsonObject) {
                        val childNode = ArgumentTreeNode("", false).fromJson(item.asJsonObject)
                        if (childNode != null) {
                            add(childNode)
                        }
                    }
                }
            }
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

    override fun isChecked(): Boolean {
        if (singleChoice) {
            for (child in childrenArgs()) {
                if (child.isChecked) {
                    return true
                }
            }
            return false
        }
        return super.isChecked()
    }

    override fun getParent(): ArgumentTreeNode? {
        return super.getParent() as ArgumentTreeNode?
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