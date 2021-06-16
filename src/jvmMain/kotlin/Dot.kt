/**
 * R. C. Howell 2021
 * - https://www.graphviz.org/pdf/dotguide.pdf
 * - https://graphviz.org/doc/info/lang.html
 */

import kotlin.properties.ObservableProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

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
 * graph: [ strict ] (graph | digraph) [ ID ] '{' stmt_list '}'
 */
sealed class DotGraph(
    val name: String?,
    val strict: Boolean,
    val edgeOp: DotEdgeOp
) : DotAttrCapture() {

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
     * Node statement is a "-" with an identifier string.
     * Motivation for this is unordered markdown lists using "-"
     * Attributes are optional with `+`
     */
    operator fun String.unaryMinus(): DotNodeStmt {
        val stmt = DotNodeStmt(DotNodeId(this))
        stmts.add(stmt)
        return stmt
    }

    /**
     * I couldn't figure out how to chain edges while also attaching edge attributes
     *  hence why these return a DotEdgeStmt and not the rhs
     */

    /**
     * Node to Node
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

    /**
     * Node to Subgraph
     */
    infix operator fun String.minus(target: DotSubgraph): DotEdgeStmt {
        val lhs = DotNodeId(this)
        val stmt = when (edgeOp) {
            DotEdgeOp.DIR -> DotEdgeStmt(DotEdge.NodeDirSubgraph(lhs, target))
            DotEdgeOp.UNDIR -> DotEdgeStmt(DotEdge.NodeUnDirSubgraph(lhs, target))
        }
        stmts.add(stmt)
        return stmt
    }

    /**
     * Subgraph to Node
     */
    infix operator fun DotSubgraph.minus(target: String): DotEdgeStmt {
        val rhs = DotNodeId(target)
        val stmt = when (edgeOp) {
            DotEdgeOp.DIR -> DotEdgeStmt(DotEdge.SubgraphDirNode(this, rhs))
            DotEdgeOp.UNDIR -> DotEdgeStmt(DotEdge.SubgraphUnDirNode(this, rhs))
        }
        stmts.add(stmt)
        return stmt
    }

    /**
     * Subgraph to Subgraph
     */
    infix operator fun DotSubgraph.minus(target: DotSubgraph): DotEdgeStmt {
        val stmt = when (edgeOp) {
            DotEdgeOp.DIR -> DotEdgeStmt(DotEdge.SubgraphDirSubgraph(this, target))
            DotEdgeOp.UNDIR -> DotEdgeStmt(DotEdge.SubgraphUnDirSubgraph(this, target))
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

    var center: Boolean? by attr()

    var ratioS: String? by attr("ratio")

    var ratioD: Double? by attr("ratio")

    /**
     * Returns the Dot code for this graph
     */
    fun dot(): String = with(StringBuilder()) {
        if (strict) append("strict ")
        if (edgeOp == DotEdgeOp.UNDIR) append("graph ") else append("digraph ")
        if (name != null) append("$name ")
        appendLine("{")
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

    var rank: String? by attr()

    var style: String? by attr()

    var color: String? by attr()

    var label: String? by attr()

    override fun dot(sb: StringBuilder, indent: Int): Unit = with(sb) {
        append(INDENT.repeat(indent))
        if (name != null) appendLine("subgraph $name {") else appendLine("subgraph {")
        if (attrs.isNotEmpty()) appendLine(attrs.dot(indent, "\n"))
        stmts.forEach { stmt ->
            stmt.dot(this, indent + 1)
            append("\n")
        }
        append(INDENT.repeat(indent)).append("}")
    }
}

/**
 * Interface for code gen.
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
 */
class DotNodeId(val id: String, private val port: DotPortPos? = null) : DotVertex {
    override fun dot(sb: StringBuilder, indent: Int): Unit = with(sb) {
        if (port == null) {
            append(id)
        } else {
            append("$id $port")
        }
    }
}

/**
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
enum class DotEdgeOp(val v: String) {
    DIR("->"),
    UNDIR("--");

    override fun toString(): String = v
}

/**
 * Eight variants of a node connection
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
        from.dot(this)
        append(" $op ")
        to.dot(this)
    }
}

/**
 * Extension function to print a set of key values in the Dot language
 */
fun MutableMap<String, Any>.dot(indent: Int = 0, separator: String = ","): String {
    val prefix = INDENT.repeat(indent)
    return this@dot.map { (k, v) ->
        val valueString = when (v) {
            is String -> "\"$v\""
            else -> v.toString()
        }
        "$prefix$k=$valueString"
    }.joinToString(separator) { it }
}

/**
 * This has turned into generic logic for saving non-null object properties with a key override
 */
abstract class DotAttrCapture {

    var attrs = mutableMapOf<String, Any>()

    /**
     * Inspired by the Delegate.observable
     */
    fun <T> attr(nameOverride: String? = null): ReadWriteProperty<Any?, T?> = object : ObservableProperty<T?>(null) {
        override fun afterChange(property: KProperty<*>, oldValue: T?, newValue: T?) {
            val name = nameOverride ?: property.name
            if (newValue == null) {
                attrs.remove(name)
            } else {
                attrs[name] = newValue
            }
        }
    }
}

/**
 * Dot attributes. http://www.graphviz.org/doc/info/attrs.html
 * attr_stmt: (graph | node | edge) attr_list
 */
sealed class DotAttrStmt : DotStmt, DotAttrCapture() {

    /**
     * Attribute statements not associated with a particular graph, node, or edge
     */
    abstract val standalone: Boolean

    override fun dot(sb: StringBuilder, indent: Int): Unit = with(sb) {
        if (standalone) {
            append(INDENT.repeat(indent))
            append(
                when (this@DotAttrStmt) {
                    is DotNodeAttrStmt -> "node"
                    is DotGraphAttrStmt -> "graph"
                    is DotEdgeAttrStmt -> "edge"
                }
            )
        }
        if (attrs.isEmpty()) {
            // Only print empty brackets on a standalone attribute statement
            if (standalone) append("[]")
            return
        }
        append("[").append(attrs.dot()).append("]")
    }
}

/**
 * Graph specific attribute statement
 */
class DotGraphAttrStmt(override val standalone: Boolean = false) : DotAttrStmt() {

    var color: String? by attr()

    var style: String? by attr()
}

/**
 * Node specific attribute statement.
 * All functions are node attributes.
 */
class DotNodeAttrStmt(override val standalone: Boolean = false) : DotAttrStmt() {

    var color: String? by attr()

    var label: String? by attr()

    var shape: String? by attr()

    var style: String? by attr()
}

/**
 * Edge specific attribute statement
 */
class DotEdgeAttrStmt(override val standalone: Boolean = false) : DotAttrStmt() {

    var arrowhead: DotArrowType? by attr()

    var arrowsize: Double? by attr()

    var arrowtail: DotArrowType? by attr()

    var clazz: String? by attr("class")

    var color: String? by attr()

    var colorscheme: String? by attr()

    var comment: String? by attr()

    var constraint: Boolean? by attr()

    var decorate: Boolean? by attr()

    var dir: DotDirType? by attr()

    var edgehref: String? by attr()

    var edgetarget: String? by attr()

    var edgetooltip: String? by attr()

    var edgeurl: String? by attr()

    var fillcolor: String? by attr()

    var fontcolor: String? by attr()

    var fontname: String? by attr()

    var fontsize: Double? by attr()

    var headlp2: Pair<Double, Double>? by attr("head_lp")

    var headlp3: Triple<Double, Double, Double>? by attr("head_lp")

    var headclip: Boolean? by attr()

    var headhref: String? by attr()

    var headlabel: String? by attr()

    var headPort: DotPortPos? by attr()

    var headtarget: String? by attr()

    var headtooltip: String? by attr()

    var headurl: String? by attr()

    var href: String? by attr()

    var id: String? by attr()

    var label: String? by attr()

    var labelangle: Double? by attr()

    var labeldistance: Double? by attr()

    var labelfloat: Boolean? by attr()

    var labelfontcolor: String? by attr()

    var labelfontname: String? by attr()

    var tailport: String? by attr()

    var headport: String? by attr()
}

enum class DotArrowType {
    NORMAL,
    DOT,
    ODOT,
    NONE,
    EMPTY,
    DIAMOND,
    EDIAMOND,
    BOX,
    OPEN,
    VEE,
    INV,
    INVDOT,
    INVODOT,
    TEE,
    INVEMPTY,
    ODIAMOND,
    CROW,
    OBOX,
    HALFOPEN;

    override fun toString(): String = super.toString().toLowerCase()
}

enum class DotDirType {
    FORWARD,
    NONE;

    override fun toString(): String = super.toString().toLowerCase()
}

enum class DotPortPos {
    N,
    NE,
    E,
    SE,
    S,
    SW,
    W,
    NW,
    C,
    DEF;

    override fun toString(): String = when (this) {
        DEF -> "_"
        else -> super.toString().toLowerCase()
    }
}
