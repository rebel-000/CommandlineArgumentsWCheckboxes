package com.github.rebel000.cmdlineargs

import com.google.gson.JsonArray
import com.google.gson.JsonObject

class ArgumentTreeNode(var name: String, var isFolder: Boolean) : com.intellij.ui.CheckedTreeNode() {
    fun toJson(): JsonObject {
        val result = JsonObject()
        result.addProperty("name", name)
        result.addProperty("checked", isChecked)
        if (isFolder) {
            val items = JsonArray(childCount)
            result.add("items", items)
            for (child in children()) {
                if (child is ArgumentTreeNode) {
                    items.add(child.toJson())
                }
            }
        }
        return result
    }

    companion object {
        fun fromJson(json: JsonObject): ArgumentTreeNode? {
            val nameProperty = json.get("name")
            if (nameProperty != null) {
                val name = nameProperty.asString
                val isFolder = json.has("items")
                val node = ArgumentTreeNode(name, isFolder)
                node.setChecked(json.get("checked")?.asBoolean == true)
                return node;
            }
            return null;
        }
    }

    override fun toString(): String {
        return name
    }
}