package com.github.rebel000.cmdlineargs

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.intellij.openapi.project.Project
import com.intellij.ui.CheckboxTree
import com.intellij.ui.CheckedTreeNode
import com.jetbrains.rider.projectView.solutionDirectory
import com.jetbrains.rider.projectView.solutionName
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.io.File
import java.util.*
import javax.swing.DropMode
import javax.swing.JComponent
import javax.swing.JTree
import javax.swing.TransferHandler
import javax.swing.event.TreeModelEvent
import javax.swing.event.TreeModelListener
import javax.swing.tree.*

class ArgumentTree(private val project: Project): CheckboxTree(ArgumentTreeCellRenderer(), null), TreeModelListener {
    private val nodeFlavor = DataFlavor(DataFlavor.javaJVMLocalObjectMimeType + ";class=\"" + Array<DefaultMutableTreeNode>::class.java.name + "\"")
    private val supportedFlavors = arrayOf<DataFlavor?>(nodeFlavor)
    private val argsService = ArgumentTreeService.getInstance(project)
    private val settingsFileName: String = project.solutionDirectory.path + "/" + project.solutionName + ".ddargs.json"
    private val rootNode = DefaultMutableTreeNode(Resources.message("toolwindow.rootNode"))
    private val myModel = DefaultTreeModel(rootNode)
    private var isLocked: Boolean = false

    init {
        this.dragEnabled = true
        this.dropMode = DropMode.ON_OR_INSERT
        this.myModel.addTreeModelListener(this)
        this.model = myModel;
        loadState()

        this.transferHandler = object : TransferHandler() {
            override fun canImport(support: TransferSupport): Boolean {
                if (!support.isDrop || !support.isDataFlavorSupported(nodeFlavor)) {
                    return false
                }
                support.setShowDropLocation(true)
                val selectedNodes = selectedNodes()
                var destNode = (support.dropLocation as JTree.DropLocation).path.lastPathComponent as TreeNode
                if (destNode == rootNode) {
                    for (selectedNode in selectedNodes) {
                        if (selectedNode.parent == rootNode) {
                            return false
                        }
                    }
                }
                else {
                    while (destNode is ArgumentTreeNode) {
                        if (selectedNodes.contains(destNode)) {
                            return false
                        }
                        destNode = destNode.parent
                    }
                }
                return support.dropAction == MOVE
            }

            override fun createTransferable(component: JComponent): Transferable? {
                if (selectionCount > 0) {
                    return ArgumentTreeTransferable(selectedNodes())
                }
                return null
            }

            override fun getSourceActions(c: JComponent): Int = MOVE

            override fun importData(support: TransferSupport): Boolean {
                if (!canImport(support) || !support.transferable.isDataFlavorSupported(nodeFlavor)) {
                    return false
                }
                var index: Int
                val parent: DefaultMutableTreeNode
                val destNode = (support.dropLocation as JTree.DropLocation).path.lastPathComponent as DefaultMutableTreeNode
                if (destNode is ArgumentTreeNode && !destNode.isFolder) {
                    parent = destNode.parent as DefaultMutableTreeNode
                    index = myModel.getIndexOfChild(parent, destNode)
                }
                else {
                    parent = destNode
                    index = destNode.childCount
                }
                @Suppress("UNCHECKED_CAST")
                val nodes = support.transferable.getTransferData(nodeFlavor) as Array<DefaultMutableTreeNode>
                for (node in nodes) {
                    myModel.removeNodeFromParent(node)
                    myModel.insertNodeInto(node, parent, index++)
                }
                expandPath(TreePath(parent.path))
                return true
            }
        }

    }

    fun addNode(node: ArgumentTreeNode, parent: ArgumentTreeNode? = null) {
        if (parent != null) {
            if (parent.isFolder) {
                myModel.insertNodeInto(node, parent, parent.childCount)
            }
            else {
                myModel.insertNodeInto(node, parent.parent as DefaultMutableTreeNode, parent.parent.childCount)
            }
        }
        else {
            myModel.insertNodeInto(node, rootNode, rootNode.childCount)
            if (rootNode.childCount == 1) {
                myModel.reload()
            }
        }
    }

    fun removeNode(node: ArgumentTreeNode) {
        if (node.parent != null)
        {
            myModel.removeNodeFromParent(node)
        }
    }

    fun selectedNodes(): Array<ArgumentTreeNode> {
        return this.getSelectedNodes(ArgumentTreeNode::class.java, null)
    }

    fun firstSelectedNode(): ArgumentTreeNode? {
        val selectedNodes = this.getSelectedNodes(ArgumentTreeNode::class.java, null)
        return if (selectedNodes.isEmpty()) { null } else { selectedNodes[0] }
    }

    fun moveNodesUp() {
        lock()
        val selectedNodes = selectedNodes()
        for (node in selectedNodes) {
            if (node.parent != null) {
                val parent = node.parent
                val index = parent.getIndex(node)
                val selIndex = this.selectionRows[0]
                if (index > 0) {
                    val neighbor = myModel.getChild(parent, index - 1) as ArgumentTreeNode
                    if (neighbor.isFolder) {
                        myModel.removeNodeFromParent(node)
                        myModel.insertNodeInto(node, neighbor, neighbor.childCount)
                    }
                    else {
                        myModel.removeNodeFromParent(node)
                        myModel.insertNodeInto(node, parent as DefaultMutableTreeNode, index - 1)
                    }
                } else {
                    if (parent.parent != null) {
                        val parentIndex = parent.parent.getIndex(parent)
                        myModel.removeNodeFromParent(node)
                        myModel.insertNodeInto(node, parent.parent as DefaultMutableTreeNode, parentIndex)
                    }
                }
                this.addSelectionRow(selIndex - 1)
            }
        }
        unlock(true)
    }

    fun moveNodesDown() {
        lock()
        val selectedNodes = selectedNodes()
        for (node in selectedNodes) {
            if (node.parent != null) {
                val parent = node.parent
                val index = parent.getIndex(node)
                var selIndex = this.selectionRows[0]
                if (index < parent.childCount - 1) {
                    val neighbor = myModel.getChild(parent, index + 1) as ArgumentTreeNode
                    if (neighbor.isFolder) {
                        myModel.removeNodeFromParent(node)
                        myModel.insertNodeInto(node, neighbor, 0)
                    }
                    else {
                        myModel.removeNodeFromParent(node)
                        myModel.insertNodeInto(node, parent as DefaultMutableTreeNode, index + 1)
                        ++selIndex
                    }
                } else {
                    if (parent.parent != null) {
                        val parentIndex = parent.parent.getIndex(parent)
                        myModel.removeNodeFromParent(node)
                        myModel.insertNodeInto(node, parent.parent as DefaultMutableTreeNode, parentIndex + 1)
                    }
                }

                expandPath(TreePath((node.parent as DefaultMutableTreeNode).path))
                this.addSelectionRow(selIndex)
            }
        }
        unlock(true)
    }

    private fun updateCommandArgs() {
        if (!isLocked) {
            val args = Vector<String>(rootNode.childCount)
            for (child in rootNode.children()) {
                if (child is ArgumentTreeNode) {
                    processNode(args, child)
                }
            }
            argsService.arguments = args.joinToString(" ")
        }
    }

    private fun processNode(args: Vector<String>, node: ArgumentTreeNode) {
        if (node.isFolder) {
            args.ensureCapacity(args.size + node.childCount)
            for (child in node.children()) {
                if (child is ArgumentTreeNode) {
                    processNode(args, child)
                }
            }
        }
        else if (node.isChecked) {
            args.add(node.name)
        }
    }

    private fun lock() {
        isLocked = true
    }

    private fun unlock(shouldSave: Boolean = false) {
        isLocked = false
        updateCommandArgs()
        if (shouldSave) {
            saveState()
        }
    }

    private fun saveState() {
        if (!isLocked) {
            val jObject = JsonObject()
            val items = JsonArray(rootNode.childCount)
            for (child in rootNode.children()) {
                if (child is ArgumentTreeNode) {
                    val jNode = child.toJson()
                    jNode.addProperty("expanded", isExpanded(TreePath(child.path)))
                    items.add(jNode)
                }
            }
            jObject.addProperty("version", 1)
            jObject.addProperty("override", argsService.shouldOverride)
            jObject.add("items", items)
            File(settingsFileName).writeText(jObject.toString())
        }
    }

    private fun loadState() {
        lock()
        val settingsFile = File(settingsFileName)
        if (settingsFile.exists()) {
            val jsonString = settingsFile.readText()
            val jObject = JsonParser.parseString(jsonString).asJsonObject
            val version = jObject.get("version")
            if (version != null && version.asInt == 1) {
                val override = jObject.get("override")
                argsService.shouldOverride = override != null && override.asBoolean
                jObject.addProperty("override", argsService.shouldOverride)
                rootNode.removeAllChildren()
                loadChildNodes(null, jObject.getAsJsonArray("items"))
                myModel.reload()
            }
        }
        unlock()
    }

    private fun loadChildNodes(parent: ArgumentTreeNode?, items: JsonArray?) {
        if (items != null) {
            for (item in items) {
                if (item.isJsonObject) {
                    val jObject = item.asJsonObject
                    val node = ArgumentTreeNode.fromJson(jObject)
                    if (node != null) {
                        addNode(node, parent)
                        if (node.isFolder) {
                            loadChildNodes(node, jObject.getAsJsonArray("items"))
                        }
                        val expanded = jObject.get("expanded")
                        if (expanded != null && expanded.asBoolean) {
                            expandPath(TreePath(node.path))
                        }
                    }
                }
            }
        }
    }

    override fun onDoubleClick(node: CheckedTreeNode?) {
        super.onDoubleClick(node)

        if (node is ArgumentTreeNode) {
            if (ArgumentNodePropertiesDialog(node).showAndGet()) {
                if (!node.isFolder && node.childCount > 0) {
                    val folder = node.parent as DefaultMutableTreeNode
                    var index = folder.getIndex(node) + 1
                    while (node.childCount > 0) {
                        val child = node.children().nextElement() as DefaultMutableTreeNode
                        myModel.removeNodeFromParent(child)
                        myModel.insertNodeInto(child, folder, index)
                        index++
                    }
                }
                myModel.nodeChanged(node)
            }
        }

        saveState()
    }

    override fun treeNodesChanged(e: TreeModelEvent?) {
        saveState()
        updateCommandArgs()
    }

    override fun treeNodesInserted(e: TreeModelEvent?) {
        saveState()
        updateCommandArgs()
    }

    override fun treeNodesRemoved(e: TreeModelEvent?) {
        saveState()
        updateCommandArgs()
    }

    override fun treeStructureChanged(e: TreeModelEvent?) {
        saveState()
        updateCommandArgs()
    }

    inner class ArgumentTreeTransferable(private var nodes: Array<ArgumentTreeNode>) : Transferable {
        @Throws(UnsupportedFlavorException::class)
        override fun getTransferData(flavor: DataFlavor): Any {
            if (!isDataFlavorSupported(flavor)) throw UnsupportedFlavorException(flavor)
            return nodes
        }

        override fun getTransferDataFlavors(): Array<DataFlavor?> {
            return supportedFlavors
        }

        override fun isDataFlavorSupported(flavor: DataFlavor): Boolean {
            return flavor.equals(nodeFlavor)
        }
    }
}