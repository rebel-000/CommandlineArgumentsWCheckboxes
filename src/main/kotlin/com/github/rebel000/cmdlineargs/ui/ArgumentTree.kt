package com.github.rebel000.cmdlineargs.ui

import com.github.rebel000.cmdlineargs.ArgumentFilter
import com.github.rebel000.cmdlineargs.ArgumentsService
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.intellij.ide.dnd.TransferableList
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.project.Project
import com.intellij.ui.*
import com.intellij.util.ui.EditableModel
import com.jetbrains.rider.projectView.solutionDirectory
import com.jetbrains.rider.projectView.solutionName
import java.awt.datatransfer.Transferable
import java.io.File
import java.util.*
import javax.swing.*
import javax.swing.event.TreeExpansionEvent
import javax.swing.event.TreeExpansionListener
import javax.swing.event.TreeModelEvent
import javax.swing.event.TreeModelListener
import javax.swing.tree.*
import kotlin.collections.ArrayList
import kotlin.math.min

@Suppress("DialogTitleCapitalization")
class ArgumentTree(private val project: Project) :
    CheckboxTree(ArgumentTreeCellRenderer(), null, CheckPolicy(true, true, true, true)),
    TreeModelListener,
    TreeExpansionListener {
    private val argsService = ArgumentsService.getInstance(project)
    private val settingsFileName: String = project.solutionDirectory.path + "/" + project.solutionName + ".ddargs.json"
    private val rootNode = ArgumentTreeRootNode()
    private var lockedCounter: Int = 0
    private var hasChanges = false
    private val myModel: ArgumentTreeModel get() = model as ArgumentTreeModel
    private var isLocked: Boolean
        get() = lockedCounter == 0
        set(value) {
            if (value) {
                lockedCounter++
            }
            else {
                lockedCounter--
            }
        }

    override fun setTransferHandler(handler: TransferHandler?) {
        super.setTransferHandler(handler)
    }

    init {
        model = ArgumentTreeModel()
        myModel.addTreeModelListener(this)
        addTreeExpansionListener(this)
    }

    fun postInit() {
        transferHandler = object : TransferHandler() {
            override fun createTransferable(component: JComponent): Transferable? {
                val tree = component as? JTree ?: return null
                val selection = tree.selectionPaths ?: return null
                if (selection.size <= 1) return null
                return object : TransferableList<TreePath>(*selection) {
                    override fun toString(path: TreePath): String {
                        return path.lastPathComponent.toString()
                    }
                }
            }

            override fun getSourceActions(c: JComponent) = COPY_OR_MOVE
        }

        reloadState()
    }

    fun addNode(node: ArgumentTreeNode, parent: ArgumentTreeNode? = null) {
        val shouldReload = rootNode.childCount == 0
        val (folder, index) = getInsertPosition(parent)
        myModel.insertNodeInto(node, folder, index)
        if (shouldReload) {
            myModel.reload()
        }
    }

    fun insertNode(node: ArgumentTreeNode, parent: ArgumentTreeNode, index: Int = Int.MAX_VALUE) {
        myModel.insertNodeInto(node, parent, min(index, parent.childCount))
    }

    fun editNode(node: ArgumentTreeNode) {
        if (ArgumentPropertiesDialog(node).showAndGet()) {
            if (node.isFolder) {
                var firstCheckedFound = false
                if (node.singleChoice) {
                    for (child in node.childrenArgs().toList()) {
                        if (child.isChecked) {
                            if (firstCheckedFound) {
                                setNodeState(child, false)
                            } else {
                                firstCheckedFound = true
                            }
                        }
                    }
                }
            } else {
                if (node.childCount > 0) {
                    val folder = node.parent!!
                    var index = folder.getIndex(node) + 1
                    for (child in node.childrenArgs().toList()) {
                        removeNode(child)
                        insertNode(child, folder, index)
                        index++
                    }
                }
            }
            myModel.nodeChanged(node)
        }
    }

    fun expandNode(node: ArgumentTreeNode, recursive: Boolean) {
        expandPath(TreePath(node.path))
        if (recursive) {
            for (child in node.childrenArgs()) {
                expandNode(child, false)
            }
        }
    }

    fun removeNode(node: ArgumentTreeNode) {
        myModel.removeNodeFromParent(node)
    }

    fun removeNodes(nodes: Array<ArgumentTreeNode>) {
        lock()
        var i = nodes.size - 1
        while (i >= 0) {
            removeNode(nodes[i])
            i--
        }
        unlock()
    }

    fun selectedNodes(sorted: Boolean = false): Array<ArgumentTreeNode> {
        if (sorted) {
            val sortedSelectionRows = selectionRows.sorted()
            val selectedNodes = ArrayList<ArgumentTreeNode>(sortedSelectionRows.count())
            for (row in sortedSelectionRows) {
                selectedNodes.add(getPathForRow(row).lastPathComponent as ArgumentTreeNode)
            }
            return selectedNodes.toArray(arrayOf())
        }
        return getSelectedNodes(ArgumentTreeNode::class.java, null)
    }

    fun selectedNode(): ArgumentTreeNode? {
        return getSelectedNodes(ArgumentTreeNode::class.java, null).firstOrNull()
    }

    fun getInsertPosition(node: ArgumentTreeNode?, isAbove: Boolean = false): InsertPosition {
        if (node != null) {
            if (node.isFolder) {
                return InsertPosition(node, node.childCount)
            }
            val parent = node.parent
            if (parent != null) {
                if (isAbove) {
                    return InsertPosition(parent, parent.getIndex(node))
                }
                return InsertPosition(parent, parent.getIndex(node) + 1)
            }
        }
        return InsertPosition(rootNode, rootNode.childCount)
    }

    fun saveState() {
        if (!isLocked) {
            val jObject = JsonObject()
            val items = JsonArray(rootNode.childCount)
            for (child in rootNode.childrenArgs()) {
                items.add(child.toJson())
            }
            jObject.addProperty("version", 1)
            jObject.addProperty("override", argsService.shouldOverride)
            jObject.add("items", items)
            File(settingsFileName).writeText(jObject.toString())
        }
    }

    fun reloadState() {
        lock()
        val settingsFile = File(settingsFileName)
        if (settingsFile.exists()) {
            val jsonString = settingsFile.readText()
            val jObject = JsonParser.parseString(jsonString).asJsonObject
            val version = jObject.get("version")
            if (version != null && version.asInt == 1) {
                rootNode.removeAllChildren()
                argsService.shouldOverride = jObject.get("override")?.asBoolean == true
                rootNode.fromJson(jObject)
            }
            myModel.reload()
            restoreExpandState(rootNode)
        }
        hasChanges = false
        unlock()
        rebuildArgs()
    }

    private fun restoreExpandState(node: ArgumentTreeNode) {
        if (node.isExpanded) {
            expandNode(node, false)
        }
        for (child in node.childrenArgs()) {
            restoreExpandState(child)
        }
    }

    fun lock() {
        isLocked = true
    }

    fun unlock() {
        isLocked = false
        if (hasChanges) {
            treeChanged()
            hasChanges = false
        }
    }

    private fun rebuildArgs() {
        argsService.arguments = Vector()
        rootNode.getArgs(argsService.arguments as Vector<String>, ArgumentFilter(project))
    }

    private fun treeChanged() {
        if (!isLocked) {
            saveState()
            rebuildArgs()
        }
        else {
            hasChanges = true
        }
    }

    override fun onDoubleClick(node: CheckedTreeNode?) {
        if (node != null) {
            editNode(node as ArgumentTreeNode)
        }
    }

    override fun onNodeStateChanged(node: CheckedTreeNode?) {
        if (node is ArgumentTreeNode) {
            if (!isLocked && node.isChecked) {
                lock()
                if (node.singleChoice) {
                    setNodeState(node, false)
                } else {
                    val parent = node.parent
                    if (parent?.singleChoice == true) {
                        for (child in parent.childrenArgs()) {
                            if (child !== node) {
                                setNodeState(child, false)
                            }
                        }
                    }
                }
                unlock()
            }
        }
    }

    override fun treeNodesChanged(e: TreeModelEvent?) {
        treeChanged()
    }

    override fun treeNodesInserted(e: TreeModelEvent?) {
        treeChanged()
    }

    override fun treeNodesRemoved(e: TreeModelEvent?) {
        treeChanged()
    }

    override fun treeStructureChanged(e: TreeModelEvent?) {
        treeChanged()
    }

    override fun treeExpanded(event: TreeExpansionEvent?) {
        ((event ?: return).path.lastPathComponent as ArgumentTreeNode).isExpanded = true
        treeChanged()
    }

    override fun treeCollapsed(event: TreeExpansionEvent?) {
        ((event ?: return).path.lastPathComponent as ArgumentTreeNode).isExpanded = false
        treeChanged()
    }

    data class InsertPosition(val parent: ArgumentTreeNode, val index: Int)

    inner class ArgumentTreeModel : DefaultTreeModel(rootNode), EditableModel,
        RowsDnDSupport.RefinedDropSupport {
        override fun addRow() {}
        override fun removeRow(index: Int) {}
        override fun exchangeRows(oldIndex: Int, newIndex: Int) {}
        override fun canExchangeRows(oldIndex: Int, newIndex: Int) = false

        override fun canDrop(
            oldIndex: Int,
            newIndex: Int,
            position: RowsDnDSupport.RefinedDropSupport.Position
        ): Boolean {
            if (oldIndex < 0 || newIndex < 0 || rowCount <= oldIndex || rowCount <= newIndex) {
                return false
            }
            val oldPaths = selectionPaths ?: return false
            val newNode = getPathForRow(newIndex).lastPathComponent as ArgumentTreeNode
            val newParent = newNode.parent
            for (oldPath in oldPaths) {
                val oldNode = oldPath.lastPathComponent as ArgumentTreeNode
                if (oldNode === newNode) {
                    return false
                }
                val oldParent = oldNode.parent
                if (oldParent === newParent) {
                    if (oldNode.previousSibling === newNode && position == RowsDnDSupport.RefinedDropSupport.Position.BELOW) {
                        return false
                    }
                    if (oldNode.nextSibling === newNode && position == RowsDnDSupport.RefinedDropSupport.Position.ABOVE) {
                        return false
                    }
                }
            }
            return true
        }

        override fun isDropInto(component: JComponent, oldIndex: Int, newIndex: Int): Boolean {
            return ((getPathForRow(newIndex) ?: return false).lastPathComponent as ArgumentTreeNode).isFolder
        }

        override fun drop(oldIndex: Int, newIndex: Int, position: RowsDnDSupport.RefinedDropSupport.Position) {
            lock()
            val dstNode = getPathForRow(newIndex).lastPathComponent as ArgumentTreeNode
            var (folder, index) = getInsertPosition(
                dstNode,
                position == RowsDnDSupport.RefinedDropSupport.Position.ABOVE
            )
            val folderPath = folder.path
            val selectedNodes = selectedNodes(true).filter{ n -> !folderPath.contains(n) }.toTypedArray()
            val moveNodes = selectedNodes.filter { n -> !n.checkIsAncestorIn(selectedNodes) }
            val newSelectionPaths = ArrayList<TreePath>(moveNodes.count())
            for (node in moveNodes) {
                val wasExpanded = isExpanded(TreePath(node.path))
                if (folder.singleChoice) {
                    setNodeState(node, false)
                }
                if (node.parent === folder && folder.getIndex(node) < index) {
                    removeNodeFromParent(node)
                    insertNodeInto(node, folder, index - 1)
                    newSelectionPaths.add(TreePath(node.path))
                } else {
                    removeNodeFromParent(node)
                    insertNodeInto(node, folder, index)
                    newSelectionPaths.add(TreePath(node.path))
                    index++
                }
                if (wasExpanded) {
                    expandPath(TreePath(node.path))
                }
            }
            expandPath(TreePath(folder.path))
            selectionPaths = newSelectionPaths.toArray(arrayOf());
            unlock()
        }
    }
}