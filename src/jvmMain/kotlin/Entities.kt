/**
 * - https://www.graphviz.org/pdf/dotguide.pdf
 * - https://graphviz.org/doc/info/lang.html
 */

/**
 * Dot entity basic interface for serialization.
 */
interface DotEntity {
    fun dot(): String
}

/**
 * graph: [ strict ] (graph | digraph) [ ID ] '{' stmt_list '}'
 */
abstract class DotGraph(private val name: String?) {

    /**
     * stmt_list: [ stmt [ ';' ] stmt_list ]
     */
    private val stmts = mutableListOf<DotStmt>()

    fun add(stmt: DotStmt) {
        stmts.add(stmt)
    }
}

/**
 * Undirected graph
 */
class DotUnDirGraph(name: String?) : DotGraph(name)

/**
 * Directed graph
 */
class DotDirGraph(name: String?) : DotGraph(name)

/**
 * stmt: node_stmt
 * | edge_stmt
 * | attr_stmt
 * | ID '=' ID
 * | subgraph
 */
abstract class DotStmt()

/**
 * ID '=' ID
 */
class DotIdStmt(val id: String, val value: String) : DotStmt()

/**
 *  node_stmt: node_id [ attr_list ]
 */
class DotNodeStmt(val nodeId: DotNodeId) : DotStmt() {
    private var attributes = mutableListOf<DotNodeAttr<Any>>()
}

/**
 * Calling a DotVertex entities that can be the source or target of an edge -- i.e. node ids and subgraphs
 */
interface DotVertex

/**
 * node_id: ID [ port ]
 *
 * TODO add port
 */
class DotNodeId(val id: String) : DotVertex

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
class DotEdgeStmt : DotStmt() {
    private val connections = mutableListOf<DotEdge>()
    private val attributes = mutableListOf<DotEdgeAttr<Any>>()
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
    val from: DotVertex,
    val to: DotVertex,
    val op: DotEdgeOp,
) {

    class NodeUnDirNode(from: DotNodeId, to: DotNodeId) : DotEdge(from, to, DotEdgeOp.UNDIR)

    class NodeDirNode(from: DotNodeId, to: DotNodeId) : DotEdge(from, to, DotEdgeOp.DIR)

    class SubgraphUnDirSubgraph(from: DotSubgraph, to: DotSubgraph) : DotEdge(from, to, DotEdgeOp.UNDIR)

    class SubgraphDirSubgraph(from: DotSubgraph, to: DotSubgraph) : DotEdge(from, to, DotEdgeOp.DIR)

    class NodeUnDirSubgraph(from: DotNodeId, to: DotSubgraph) : DotEdge(from, to, DotEdgeOp.UNDIR)

    class NodeDirSubgraph(from: DotNodeId, to: DotSubgraph) : DotEdge(from, to, DotEdgeOp.DIR)

    class SubgraphUnDirNode(from: DotSubgraph, to: DotNodeId) : DotEdge(from, to, DotEdgeOp.UNDIR)

    class SubgraphDirNode(from: DotSubgraph, to: DotNodeId) : DotEdge(from, to, DotEdgeOp.DIR)
}

/**
 * attr_stmt: (graph | node | edge) attr_list
 */
sealed class DotAttrStmt<T : DotAttr<Any>>() : DotStmt() {
    private val attrList = mutableListOf<T>()

    fun add(attr: T) {
        attrList.add(attr)
    }
}

/**
 * Graph specific attribute statement
 */
class DotGraphAttrStmt() : DotAttrStmt<DotGraphAttr<Any>>()

/**
 * Node specific attribute statement
 */
class DotNodeAttrStmt() : DotAttrStmt<DotNodeAttr<Any>>()

/**
 * Edge specific attribute statement
 */
class DotEdgeAttrStmt() : DotAttrStmt<DotEdgeAttr<Any>>()

/**
 * Dot attributes. http://www.graphviz.org/doc/info/attrs.html
 * Interfaces for each subtype allows for multiple inheritance.
 */
interface DotAttr<T> {
    val name: String
    val value: T?
    val default: T
}

/**
 * Graph attributes
 */
interface DotGraphAttr<T> : DotAttr<T>

/**
 * Node attributes
 */
interface DotNodeAttr<T> : DotAttr<T>

/**
 * Edge attributes
 */
interface DotEdgeAttr<T> : DotAttr<T>

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

    fun add(stmt: DotStmt) {
        stmts.add(stmt)
    }
}
