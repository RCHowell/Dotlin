/**
 * Dot attributes. http://www.graphviz.org/doc/info/attrs.html
 */
interface DotAttr<T> {
    val name: String
    val value: T
}

/**
 * color 	ENC 	string 	black
 */
class DotAttrColor(override val value: String) : DotAttr<String> {
    override val name = "color"
}

