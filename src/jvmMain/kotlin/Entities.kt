/**
 * - https://www.graphviz.org/pdf/dotguide.pdf
 * - https://graphviz.org/doc/info/lang.html
 */

// https://www.youtube.com/watch?v=SsoOG6ZeyUI
private const val INDENT = "  "

/**
 * Dot entity basic interface for serialization.
 */
interface DotEntity {

    /**
     * I haven't decided what I like best.
     * Passing the StringBuilder using the `with` pattern seems nice enough.
     */
    fun dot(sb: StringBuilder, indent: Int = 0)
}

/**
 * graph: [ strict ] (graph | digraph) [ ID ] '{' stmt_list '}'
 */
abstract class DotGraph(private val name: String?) {

    // Simple `graph` vs `digraph` for Dot generation
    abstract val type: String

    /**
     * stmt_list: [ stmt [ ';' ] stmt_list ]
     */
    val stmts = mutableListOf<DotStmt>()

    /**
     * Prints the Dot code for this graph
     */
    fun dot(): String = with(StringBuilder()) {
        if (name != null) appendLine("$type $name {") else appendLine("$type {")
        stmts.forEach { stmt ->
            stmt.dot(this, 1)
            append("\n")
        }
        appendLine("}")
    }.toString()

    /**
     * Node attribute statement
     * - Dot: node [style=filled,color=white];
     * - DSL: node {
     *     style(filled)
     *     color(white)
     *   }
     */
    inline fun node(f: DotNodeAttrStmt.() -> Unit) {
        val stmt = DotNodeAttrStmt(true)
        stmt.f()
        stmts.add(stmt)
    }

    /**
     * Node statement is a simple "+" with an identifier.
     * Attributes are optional with `attr`
     */
    operator fun String.unaryPlus(): DotNodeStmt {
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
        val stmt = DotEdgeStmt(DotEdge.NodeUnDirNode(lhs, rhs))
        stmts.add(stmt)
        return stmt
    }

    /**
     * Edge statement for directed node id to node id
     */
    infix fun String.to(target: String): DotEdgeStmt {
        val lhs = DotNodeId(this)
        val rhs = DotNodeId(target)
        val stmt = DotEdgeStmt(DotEdge.NodeDirNode(lhs, rhs))
        stmts.add(stmt)
        return stmt
    }
}

/**
 * graph { ... }
 */
fun graph(name: String? = null, f: DotUnDirGraph.() -> Unit): DotGraph {
    val graph = DotUnDirGraph(name)
    graph.f()
    return graph
}

/**
 * digraph { ... }
 */
fun digraph(name: String? = null, f: DotDirGraph.() -> Unit): DotGraph {
    val graph = DotDirGraph(name)
    graph.f()
    return graph
}

/**
 * Undirected graph
 */
class DotUnDirGraph(name: String?) : DotGraph(name) {
    override val type: String = "graph"
}

/**
 * Directed graph
 */
class DotDirGraph(name: String?) : DotGraph(name) {
    override val type: String = "digraph"
}

/**
 * stmt: node_stmt
 * | edge_stmt
 * | attr_stmt
 * | ID '=' ID
 * | subgraph
 */
abstract class DotStmt() : DotEntity

/**
 * ID '=' ID
 */
class DotIdStmt(private val id: String, private val value: String) : DotStmt() {
    override fun dot(sb: StringBuilder, indent: Int): Unit = with(sb) {
        append("$id = $value")
    }
}

/**
 *  node_stmt: node_id [ attr_list ]
 */
class DotNodeStmt(private val nodeId: DotNodeId) : DotStmt() {

    /**
     * Attributes for this node
     */
    private val attrStmt = DotNodeAttrStmt()

    /**
     * Adds attributes to this node statement
     */
    infix fun attr(f: DotNodeAttrStmt.() -> Unit) {
        attrStmt.f()
    }

    override fun dot(sb: StringBuilder, indent: Int): Unit = with(sb) {
        append(nodeId.id)
        attrStmt.dot(this, indent)
    }
}

/**
 * Calling a DotVertex entities that can be the source or target of an edge -- i.e. node ids and subgraphs
 */
interface DotVertex : DotEntity

/**
 * node_id: ID [ port ]
 *
 * TODO add port
 */
class DotNodeId(val id: String) : DotVertex {
    override fun dot(sb: StringBuilder, indent: Int): Unit = with(sb) {
        append(id)
    }
}

/**
 * For now, I will only add two node connections per line because I can't figure it out right now.
 * You can add multiple node connections per line in a PR if you want them.
 *
 *  Supported
 *  a - b
 *  a - c
 *
 *  Not Supported
 *  a - b - c
 *
 * edge_stmt : (node_id | subgraph) edgeRHS [ attr_list ]
 * edgeRHS   : edgeop (node_id | subgraph) [ edgeRHS ]
 *
 */
class DotEdgeStmt(private val edge: DotEdge) : DotStmt() {

    /**
     * Attributes for this edge
     */
    private val attrStmt = DotEdgeAttrStmt()

    infix fun attr(f: DotEdgeAttrStmt.() -> Unit) {
        attrStmt.f()
    }

    override fun dot(sb: StringBuilder, indent: Int): Unit = with(sb) {
        edge.dot(sb, indent)
        attrStmt.dot(sb, indent)
    }
}

/**
 * -> in directed graphs
 * -- in undirected graphs
 */
enum class DotEdgeOp {
    DIR,
    UNDIR;

    override fun toString(): String = when (this) {
        DIR -> "->"
        UNDIR -> "--"
    }
}

/**
 * Eight variants of a node connection. Subclass names should give it away.
 *
 * @property connector
 * @constructor Create empty Dot connection
 */
sealed class DotEdge(
    private val from: DotVertex,
    private val to: DotVertex,
    private val op: DotEdgeOp,
) : DotEntity {

    class NodeUnDirNode(from: DotNodeId, to: DotNodeId) : DotEdge(from, to, DotEdgeOp.UNDIR)

    class NodeDirNode(from: DotNodeId, to: DotNodeId) : DotEdge(from, to, DotEdgeOp.DIR)

    class SubgraphUnDirSubgraph(from: DotSubgraph, to: DotSubgraph) : DotEdge(from, to, DotEdgeOp.UNDIR)

    class SubgraphDirSubgraph(from: DotSubgraph, to: DotSubgraph) : DotEdge(from, to, DotEdgeOp.DIR)

    class NodeUnDirSubgraph(from: DotNodeId, to: DotSubgraph) : DotEdge(from, to, DotEdgeOp.UNDIR)

    class NodeDirSubgraph(from: DotNodeId, to: DotSubgraph) : DotEdge(from, to, DotEdgeOp.DIR)

    class SubgraphUnDirNode(from: DotSubgraph, to: DotNodeId) : DotEdge(from, to, DotEdgeOp.UNDIR)

    class SubgraphDirNode(from: DotSubgraph, to: DotNodeId) : DotEdge(from, to, DotEdgeOp.DIR)

    override fun dot(sb: StringBuilder, indent: Int): Unit = with(sb) {
        from.dot(sb)
        append(" $op ")
        to.dot(sb)
    }
}

/**
 * attr_stmt: (graph | node | edge) attr_list
 */
sealed class DotAttrStmt : DotStmt() {

    // Tells us if this attribute statment on its own, or attached to an indentifier
    abstract val type: String
    abstract val standalone: Boolean
    val attrs = mutableListOf<DotAttr<*>>()

    override fun dot(sb: StringBuilder, indent: Int): Unit = with(sb) {
        if (standalone) append("$type[") else append("[")
        append(attrs.joinToString(",") { attr -> "${attr.name}=${attr.value}" })
        append("]")
    }
}

/**
 * Graph specific attribute statement
 */
class DotGraphAttrStmt(override val standalone: Boolean = false) : DotAttrStmt() {
    override val type = "graph"
}

/**
 * Node specific attribute statement.
 * All functions are node attributes.
 */
class DotNodeAttrStmt(override val standalone: Boolean = false) : DotAttrStmt() {

    override val type = "node"

    fun color(v: String) = attrs.add(DotAttrColor(v))
}

/**
 * Edge specific attribute statement
 */
class DotEdgeAttrStmt(override val standalone: Boolean = false) : DotAttrStmt() {

    override val type = "edge"

    fun color(v: String) = attrs.add(DotAttrColor(v))
}

/**
 * Could probably be the same as the graph, but I like this extra inheritance control
 *
 * subgraph: [ subgraph [ ID ] ] '{' stmt_list '}'
 */
class DotSubgraph(val name: String?) : DotVertex {
    /**
     * stmt_list: [ stmt [ ';' ] stmt_list ]
     */
    private val stmts = mutableListOf<DotStmt>()
    override fun dot(sb: StringBuilder, indent: Int) {
        TODO("Not yet implemented")
    }
}
