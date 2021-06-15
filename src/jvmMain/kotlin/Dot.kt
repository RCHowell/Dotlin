/**
 * - https://www.graphviz.org/pdf/dotguide.pdf
 * - https://graphviz.org/doc/info/lang.html
 */

const val INDENT = "  "

/**
 * graph { ... }
 */
fun graph(name: String? = null, strict: Boolean = false, f: DotRootGraph.() -> Unit): DotRootGraph {
    val graph = DotRootGraph(name, strict, DotEdgeOp.UNDIR)
    graph.f()
    return graph
}

/**
 * digraph { ... }
 */
fun digraph(name: String? = null, strict: Boolean = false, f: DotRootGraph.() -> Unit): DotRootGraph {
    val graph = DotRootGraph(name, strict, DotEdgeOp.DIR)
    graph.f()
    return graph
}

/**
 * Interface for code gen
 */
interface DotEntity {

    /**
     * Entity adds its Dot code to the StringBuilder
     */
    fun dot(sb: StringBuilder, indent: Int = 0)
}

/**
 * stmt: node_stmt
 * | edge_stmt
 * | attr_stmt
 * | ID '=' ID
 * | subgraph
 */
interface DotStmt : DotEntity

/**
 *  node_stmt: node_id [ attr_list ]
 */
class DotNodeStmt(private val nodeId: DotNodeId) : DotStmt {

    /**
     * Attributes for this node
     */
    private val attrStmt = DotNodeAttrStmt()

    /**
     * Adds attributes to this node statement
     */
    infix operator fun plus(f: DotNodeAttrStmt.() -> Unit) = attrStmt.f()

    override fun dot(sb: StringBuilder, indent: Int): Unit = with(sb) {
        append(INDENT.repeat(indent))
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
 * Only adding two node connections per line because I can't figure it out right now.
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
class DotEdgeStmt(private val edge: DotEdge) : DotStmt {

    /**
     * Attributes for this edge
     */
    private val attrStmt = DotEdgeAttrStmt()

    infix operator fun plus(f: DotEdgeAttrStmt.() -> Unit) {
        attrStmt.f()
    }

    override fun dot(sb: StringBuilder, indent: Int): Unit = with(sb) {
        append(INDENT.repeat(indent))
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
 * Eight variants of a node connection.
 * Subclass names should give it away.
 */
sealed class DotEdge(
    private val from: DotVertex,
    private val to: DotVertex,
    private val op: DotEdgeOp,
) : DotEntity {

    class NodeUnDirNode(from: DotNodeId, to: DotNodeId) : DotEdge(from, to, DotEdgeOp.UNDIR)

    class NodeDirNode(from: DotNodeId, to: DotNodeId) : DotEdge(from, to, DotEdgeOp.DIR)

    //    class SubgraphUnDirSubgraph(from: DotSubgraph, to: DotSubgraph) : DotEdge(from, to, DotEdgeOp.UNDIR)
    //
    //    class SubgraphDirSubgraph(from: DotSubgraph, to: DotSubgraph) : DotEdge(from, to, DotEdgeOp.DIR)
    //
    //    class NodeUnDirSubgraph(from: DotNodeId, to: DotSubgraph) : DotEdge(from, to, DotEdgeOp.UNDIR)
    //
    //    class NodeDirSubgraph(from: DotNodeId, to: DotSubgraph) : DotEdge(from, to, DotEdgeOp.DIR)
    //
    //    class SubgraphUnDirNode(from: DotSubgraph, to: DotNodeId) : DotEdge(from, to, DotEdgeOp.UNDIR)
    //
    //    class SubgraphDirNode(from: DotSubgraph, to: DotNodeId) : DotEdge(from, to, DotEdgeOp.DIR)

    override fun dot(sb: StringBuilder, indent: Int): Unit = with(sb) {
        from.dot(sb)
        append(" $op ")
        to.dot(sb)
    }
}
