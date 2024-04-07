package com.github.rebel000.cmdlineargs.ui

import com.github.rebel000.cmdlineargs.ArgumentFilter
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import java.util.*

class ArgumentTreeRootNode(name: String) : ArgumentTreeNode(name, isFolder = true, readonly = true) {
    override fun getArgs(out: Vector<String>, filter: ArgumentFilter) {
        out.ensureCapacity(out.size + childCount)
        forEachArg {
            it.getArgs(out, filter)
        }
    }

    override fun toJson(): JsonObject {
        val result = JsonObject()
        val items = JsonArray(childCount)
        result.add("items", items)
        forEachArg {
            items.add(it.toJson())
        }
        return result
    }

    override fun fromJson(json: JsonObject): Boolean {
        removeAllChildren()
        val items = json.getAsJsonArray("items")
        if (items != null) {
            for (item in items) {
                if (item.isJsonObject) {
                    val childNode = ArgumentTreeNode("", isFolder = false, readonly = false)
                    if (childNode.fromJson(item.asJsonObject)) {
                        add(childNode)
                    }
                }
            }
            return true
        }
        return false
    }
}