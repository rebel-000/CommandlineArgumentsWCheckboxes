package com.github.rebel000.cmdlineargs.ui

import com.github.rebel000.cmdlineargs.ArgumentFilter
import com.github.rebel000.cmdlineargs.Resources
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import java.util.*

class ArgumentTreeRootNode : ArgumentTreeNode(Resources.message("toolwindow.rootNode"), true) {
    override fun getArgs(out: Vector<String>, filter: ArgumentFilter) {
        out.ensureCapacity(out.size + childCount)
        for (child in childrenArgs()) {
            child.getArgs(out, filter)
        }
    }

    override fun toJson(): JsonObject {
        val result = JsonObject()
        val items = JsonArray(childCount)
        result.add("items", items)
        for (child in childrenArgs()) {
            items.add(child.toJson())
        }
        return result
    }

    override fun fromJson(json: JsonObject): ArgumentTreeNode? {
        val items = json.getAsJsonArray("items")
        if (items != null) {
            for (item in items) {
                if (item.isJsonObject) {
                    val childNode = ArgumentTreeNode("", false).fromJson(item.asJsonObject)
                    if (childNode != null) {
                        add(childNode)
                    }
                }
            }
        }
        return super.fromJson(json)
    }

    override fun isChecked(): Boolean {
        return true
    }

    override fun toString(): String {
        return ArgumentTreeRootNode::class.java.name
    }
}