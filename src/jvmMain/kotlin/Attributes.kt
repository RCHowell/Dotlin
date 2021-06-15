import DotAttrStringer.attributes
import DotAttrStringer.dot
import java.lang.reflect.Field

class DotAttr<T>(
    val name: String,
    val value: T
)

/**
 * For now, you must use this in conjunction with @JvmField or else
 *  the field is a private Java field with getter/setter pair
 *  and we can't get the value with reflection
 *
 * To be continued .. a compile time annotation to add @JvmField to all Attr tags would be nice
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class Attr(val name: String = "")

fun Field.isDotAttr(): Boolean = this.isAnnotationPresent(Attr::class.java)

object DotAttrStringer {

    /**
     * Generate list of attributes from non-null @Attr annotated fields
     * that the calling obj can access. Hence the @JvmField.
     */
    fun attributes(caller: Any): List<DotAttr<*>> = caller::class.java.declaredFields
        .filter { it.isDotAttr() && it.canAccess(caller) }
        .map {
            val attr = it.getAnnotation(Attr::class.java)
            val value = it.get(caller)
            val name = if (attr.name.isBlank()) it.name else attr.name
            DotAttr(name, value)
        }
        .filter { it.value != null }

    /**
     * Add logic here for String escaping.
     * Might be useful to make public.
     */
    fun List<DotAttr<*>>.dot(indent: Int = 0, separator: String = ","): String {
        val prefix = INDENT.repeat(indent)
        return joinToString(separator) {
            val valueString = when (val v = it.value) {
                is String -> "\"$v\""
                else -> v.toString()
            }
            "$prefix${it.name}=$valueString"
        }
    }
}

/**
 * Dot attributes. http://www.graphviz.org/doc/info/attrs.html
 * attr_stmt: (graph | node | edge) attr_list
 */
sealed class DotAttrStmt : DotStmt {

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
        val attrs = attributes(this@DotAttrStmt)
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

    @Attr
    @JvmField
    var color: String? = null

    @Attr
    @JvmField
    var style: String? = null
}

/**
 * Node specific attribute statement.
 * All functions are node attributes.
 */
class DotNodeAttrStmt(override val standalone: Boolean = false) : DotAttrStmt() {

    @Attr
    @JvmField
    var color: String? = null

    @Attr
    @JvmField
    var label: String? = null

    @Attr
    @JvmField
    var shape: String? = null

    @Attr
    @JvmField
    var style: String? = null
}

/**
 * Edge specific attribute statement
 */
class DotEdgeAttrStmt(override val standalone: Boolean = false) : DotAttrStmt() {

    @Attr
    @JvmField
    var color: String? = null

    @Attr
    @JvmField
    var label: String? = null

    @Attr
    @JvmField
    var tailport: String? = null

    @Attr
    @JvmField
    var headport: String? = null
}
