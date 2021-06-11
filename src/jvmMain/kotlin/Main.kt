fun main() {

    // Basic Dot graph from Graphviz sample
    val graph = digraph {

        node {
            color("green")
        }

        +"a" attr {
            color("blue")
        }

        "a" to "b" attr {
            color("blue")
        }

        "a" to "c" attr {
            color("black")
        }

        for (i in 0..100) {
            "$i" to "${i+1}"
        }
    }

    println(graph.dot())
}
