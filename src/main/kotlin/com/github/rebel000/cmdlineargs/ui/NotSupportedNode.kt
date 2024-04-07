package com.github.rebel000.cmdlineargs.ui

import com.github.rebel000.cmdlineargs.ArgumentFilter
import com.github.rebel000.cmdlineargs.Resources
import com.google.gson.JsonObject
import java.util.*

class NotSupportedNode : ArgumentTreeNode(Resources.message("toolwindow.notSupportedNode"), isFolder = false, readonly = true) {
    override fun getArgs(out: Vector<String>, filter: ArgumentFilter) {}
    override fun toJson(): JsonObject { return JsonObject() }
    override fun fromJson(json: JsonObject): Boolean { return false }
}
