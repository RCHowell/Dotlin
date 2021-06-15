import DotAttrStringer.attributes
import DotAttrStringer.dot

/**
 * graph: [ strict ] (graph | digraph) [ ID ] '{' stmt_list '}'
 */
sealed class DotGraph(
    val name: String?,
    val strict: Boolean,
    val edgeOp: DotEdgeOp
) {

    /**
     * stmt_list: [ stmt [ ';' ] stmt_list ]
     */
    val stmts = mutableListOf<DotStmt>()

    /**
     * Node attribute statement
     * - Dot: node [style=filled,color=white];
     * - DSL: node {
     *     style = "filled"
     *     color = "white"
     *   }
     */
    inline fun node(f: DotNodeAttrStmt.() -> Unit) {
        val stmt = DotNodeAttrStmt(true)
        stmt.f()
        stmts.add(stmt)
    }

    /**
     * Subgraph Dot entity with direction inherited from parent Graph
     */
    inline fun subgraph(name: String? = null, f: DotSubgraph.() -> Unit): DotSubgraph {
        val stmt = DotSubgraph(name, edgeOp)
        stmt.f()
        stmts.add(stmt)
        return stmt
    }

    /**
     * Node statement is a simple "-" with an identifier.
     * Motivation for this is unordered markdown lists using "-"
     * Attributes are optional with `attr`
     */
    operator fun String.unaryMinus(): DotNodeStmt {
        val stmt = DotNodeStmt(DotNodeId(this))
        stmts.add(stmt)
        return stmt
    }

    /**
     * Edge statement for undirected node id to node id
     */
    infix operator fun String.minus(target: String): DotEdgeStmt {
        val lhs = DotNodeId(this)
        val rhs = DotNodeId(target)
        val stmt = when (edgeOp) {
            DotEdgeOp.DIR -> DotEdgeStmt(DotEdge.NodeDirNode(lhs, rhs))
            DotEdgeOp.UNDIR -> DotEdgeStmt(DotEdge.NodeUnDirNode(lhs, rhs))
        }
        stmts.add(stmt)
        return stmt
    }
}

class DotRootGraph(
    name: String?,
    strict: Boolean,
    edgeOp: DotEdgeOp
) : DotGraph(name, strict, edgeOp) {

    @Attr
    @JvmField
    var center: Boolean? = null

    /**
     * Returns the Dot code for this graph
     */
    fun dot(): String = with(StringBuilder()) {
        val type = when (edgeOp) {
            DotEdgeOp.UNDIR -> "graph"
            DotEdgeOp.DIR -> "digraph"
        }
        val prefix = if (strict) "strict $type" else type
        if (name != null) appendLine("$prefix $name {") else appendLine("$type {")
        val attrs = attributes(this@DotRootGraph)
        if (attrs.isNotEmpty()) appendLine(attrs.dot(0, "\n"))
        stmts.forEach { stmt ->
            stmt.dot(this, 1)
            append("\n")
        }
        appendLine("}")
        toString()
    }
}

/**
 * subgraph: [ subgraph [ ID ] ] '{' stmt_list '}'
 */
class DotSubgraph(name: String?, edgeOp: DotEdgeOp) : DotGraph(name, false, edgeOp), DotVertex, DotStmt {

    @Attr
    @JvmField
    var rank: String? = null

    @Attr
    @JvmField
    var style: String? = null

    @Attr
    @JvmField
    var color: String? = null

    @Attr
    @JvmField
    var label: String? = null

    override fun dot(sb: StringBuilder, indent: Int): Unit = with(sb) {
        append(INDENT.repeat(indent))
        if (name != null) appendLine("subgraph $name {") else appendLine("subgraph {")
        val attrs = attributes(this@DotSubgraph)
        if (attrs.isNotEmpty()) appendLine(attrs.dot(indent, "\n"))
        stmts.forEach { stmt ->
            stmt.dot(this, indent + 1)
            append("\n")
        }
        append(INDENT.repeat(indent)).append("}")
    }
}
