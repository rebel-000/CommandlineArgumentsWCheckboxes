package com.github.rebel000.cmdlineargs.ui

import com.github.rebel000.cmdlineargs.ArgumentsService
import com.intellij.ide.dnd.TransferableList
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.*
import com.intellij.util.ui.EditableModel
import java.awt.datatransfer.Transferable
import java.awt.event.*
import javax.swing.*
import javax.swing.event.TreeExpansionEvent
import javax.swing.event.TreeExpansionListener
import javax.swing.event.TreeModelEvent
import javax.swing.event.TreeModelListener
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.MutableTreeNode
import javax.swing.tree.TreePath
import kotlin.math.min

class ArgumentTree(private val project: Project) : CheckboxTree(ArgumentTreeCellRenderer(), null, CHECK_POLICY) {
    companion object {
        val CHECK_POLICY = CheckPolicy(false, false, false, false)
        fun getInstance(project: Project?): ArgumentTree? {
            if (project == null || project.isDisposed) {
                return null
            }
            val component = ToolWindowManager.getInstance(project).getToolWindow(ArgumentTreeView.COMMANDLINEARGS_TOOLWINDOW_ID)
                ?.contentManagerIfCreated
                ?.selectedContent
                ?.component
            return if (component is ArgumentTreeView) component.tree else null
        }
    }

    private var isLoading: Boolean = false
    private val argumentsService: ArgumentsService get() = ArgumentsService.getInstance(project)
    private val myModel: ArgumentTreeModel get() = model as ArgumentTreeModel

    init {
        showsRootHandles = false
        model = ArgumentTreeModel(argumentsService.rootNode)
        cellEditor = ArgumentTreeCellEditor(this)
        isEditable = true

        addTreeExpansionListener(object : TreeExpansionListener {
            override fun treeExpanded(event: TreeExpansionEvent?) {
                val node = event?.path?.lastPathComponent
                if (node is ArgumentTreeNode) {
                    node.isExpanded = true
                    if (!isLoading) {
                        argumentsService.scheduleSaveState()
                    }
                }
            }

            override fun treeCollapsed(event: TreeExpansionEvent?) {
                val node = event?.path?.lastPathComponent
                if (node is ArgumentTreeNode) {
                    node.isExpanded = false
                    if (!isLoading) {
                        argumentsService.scheduleSaveState()
                    }
                }
            }
        })

        addMouseListener(object : MouseAdapter() {
            override fun mouseReleased(e: MouseEvent?) {
                if (!isEditing && e?.button == MouseEvent.BUTTON3) {
                    val node = selectedNode()
                    if (node is ArgumentTreeNode && !node.readonly) {
                        val bounds = getPathBounds(TreePath(node.path)) ?: return
                        if (e.y >= bounds.y && e.y <= bounds.y + bounds.height) {
                            editNode(node)
                        }
                    }
                }
            }
        })

        model.addTreeModelListener(object : TreeModelListener {
            override fun treeNodesChanged(e: TreeModelEvent?) {
                if (!isLoading) {
                    argumentsService.scheduleSaveState()
                }
            }

            override fun treeNodesInserted(e: TreeModelEvent?) {
                if (!isLoading) {
                    argumentsService.scheduleSaveState()
                }
            }

            override fun treeNodesRemoved(e: TreeModelEvent?) {
                if (!isLoading) {
                    argumentsService.scheduleSaveState()
                }
            }

            override fun treeStructureChanged(e: TreeModelEvent?) {
                if (!isLoading) {
                    argumentsService.scheduleSaveState()
                }
            }
        })
    }

    internal fun postInit() {
        transferHandler = object : TransferHandler() {
            override fun createTransferable(component: JComponent): Transferable? {
                val tree = component as? JTree
                val selection = tree?.selectionPaths
                if (selection?.isEmpty() != false) return null
                return object : TransferableList<TreePath>(*selection) {
                    override fun toString(path: TreePath): String {
                        return path.lastPathComponent.toString()
                    }
                }
            }

            override fun getSourceActions(c: JComponent) = COPY_OR_MOVE
        }
        postLoad()
    }

    internal fun postLoad() {
        isLoading = true
        myModel.reload()
        restoreExpandState(argumentsService.rootNode)
        isLoading = false
    }

    fun addNode(node: ArgumentTreeNode, parent: ArgumentTreeNode? = null) {
        val (folder, index) = getInsertPosition(parent)
        myModel.insertNodeInto(node, folder, index)
    }

    fun addNode(node: NotSupportedNode) {
        myModel.insertNodeInto(node, argumentsService.rootNode, 0)
    }

    fun editNode(node: ArgumentTreeNode) {
        if (ArgumentPropertiesDialog(project, node).showAndGet()) {
            if (node.isFolder) {
                var firstCheckedFound = false
                if (node.singleChoice) {
                    node.forEachArg {
                        if (it.isChecked) {
                            if (firstCheckedFound) {
                                setNodeState(it, false)
                            } else {
                                firstCheckedFound = true
                            }
                        }
                    }
                }
            } else {
                if (node.childCount > 0) {
                    val parent = node.parent as ArgumentTreeNode
                    var index = parent.getIndex(node) + 1
                    node.forEachArg {
                        removeNode(it)
                        insertNode(it, parent, index)
                        index++
                    }
                }
            }
            myModel.nodeChanged(node)
        }
    }

    fun expandNode(node: ArgumentTreeNode) {
        expandPath(TreePath(node.path))
    }

    fun insertNode(node: ArgumentTreeNode, parent: ArgumentTreeNode, index: Int = Int.MAX_VALUE) {
        myModel.insertNodeInto(node, parent, min(index, parent.childCount))
    }

    override fun isPathEditable(path: TreePath?): Boolean {
        val node = path?.lastPathComponent
        return node is ArgumentTreeNode && !node.readonly
    }

    fun removeNode(node: MutableTreeNode) {
        if (node is ArgumentTreeNode && !node.readonly) {
            myModel.removeNodeFromParent(node)
        }
    }

    fun removeNodes(nodes: List<MutableTreeNode>) {
        nodes.forEach { removeNode(it) }
    }

    fun selectedNode(): ArgumentTreeNode? {
        return getSelectedNodes(ArgumentTreeNode::class.java, null).firstOrNull()
    }

    fun selectedNodes(sorted: Boolean = false): List<ArgumentTreeNode> {
        if (sorted) {
            val selectionRows = selectionRows
            if (selectionRows?.isNotEmpty() == true) {
                val sortedRows = selectionRows.sorted()
                val nodes = ArrayList<ArgumentTreeNode>(sortedRows.count())
                for (row in sortedRows) {
                    val node = getPathForRow(row).lastPathComponent
                    if (node is ArgumentTreeNode) {
                        nodes.add(node)
                    }
                }
                return nodes.toList()
            }
            return emptyList()
        }
        return getSelectedNodes(ArgumentTreeNode::class.java, null).toList()
    }

    fun getInsertPosition(node: ArgumentTreeNode?, isAbove: Boolean = false): InsertPosition {
        if (node == argumentsService.sharedRoot) {
            return InsertPosition(argumentsService.sharedRoot, argumentsService.sharedRoot.childCount)
        }
        if (node != null) {
            if (node.isFolder) {
                return InsertPosition(node, node.childCount)
            }
            val parent = node.parent
            if (parent is ArgumentTreeNode) {
                var index = parent.getIndex(node)
                if (!isAbove) {
                    ++index
                }
                return InsertPosition(parent, index)
            }
        }
        return InsertPosition(argumentsService.localRoot, argumentsService.localRoot.childCount)
    }

    fun restoreExpandState(node: ArgumentTreeNode) {
        if (node.isExpanded) {
            expandNode(node)
        }
        node.forEachArg { restoreExpandState(it) }
    }

    data class InsertPosition(val parent: ArgumentTreeNode, val index: Int)

    inner class ArgumentTreeModel(rootNode: ArgumentTreeRootNode) : DefaultTreeModel(rootNode), EditableModel, RowsDnDSupport.RefinedDropSupport {
        override fun addRow() {}
        override fun removeRow(index: Int) {}
        override fun exchangeRows(oldIndex: Int, newIndex: Int) {}
        override fun canExchangeRows(oldIndex: Int, newIndex: Int) = false

        override fun canDrop(oldIndex: Int,newIndex: Int,position: RowsDnDSupport.RefinedDropSupport.Position): Boolean {
            if (oldIndex < 0 || newIndex < 0 || rowCount <= oldIndex || rowCount <= newIndex) {
                return false
            }
            val oldPaths = selectionPaths ?: return false
            val newNode = getPathForRow(newIndex).lastPathComponent
            if (newNode !is ArgumentTreeNode) {
                return false
            }
            val newParent = newNode.parent
            if (newParent == root && position != RowsDnDSupport.RefinedDropSupport.Position.INTO) {
                return false
            }
            for (oldPath in oldPaths) {
                val oldNode = oldPath.lastPathComponent
                if (oldNode !is ArgumentTreeNode) {
                    return false
                }
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
            val node = getPathForRow(newIndex)?.lastPathComponent
            return node is ArgumentTreeNode && node.isFolder
        }

        override fun drop(oldIndex: Int, newIndex: Int, position: RowsDnDSupport.RefinedDropSupport.Position) {
            val dstNode = getPathForRow(newIndex).lastPathComponent as ArgumentTreeNode
            val isAbove = position == RowsDnDSupport.RefinedDropSupport.Position.ABOVE
            var (folder, index) = getInsertPosition(dstNode, isAbove)
            val folderPath = folder.path
            val selectedNodes = selectedNodes(true).filter { !folderPath.contains(it) }
            val moveNodes = selectedNodes.filter { !it.checkIsAncestorIn(selectedNodes) && !it.readonly }
            val newSelectionPaths = ArrayList<TreePath>(moveNodes.count())
            for (node in moveNodes) {
                val wasExpanded = isExpanded(TreePath(node.path))
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
            selectionPaths = newSelectionPaths.toArray(arrayOf())
        }
    }
}