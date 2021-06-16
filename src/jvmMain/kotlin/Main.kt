fun main() {

    val g1 = digraph {

        node {
            color = "green"
        }

        center = true

        -"a" + {
            color = "blue"
        }

        "a" - "b" + {
            color = "orange"
        }

        "a" - "c" + {
            color = "black"
        }

        subgraph {
            node {
                color = "yellow"
            }
            "e" - "f"
        }
    }

    val g2 = digraph("g") {
        node {
            shape = "plaintext"
            color = "blue"
        }
        "A1" - "B1"
        "A2" - "B2"
        "A3" - "B3"

        "A1" - "A2" + { label = "f" }
        "A2" - "A3" + { label = "g" }
        "B2" - "B3" + { label = "g'" }
        "B1" - "B3" + {
            label = "(g o f)'"
            tailport = "s"
            headport = "s"
        }

        subgraph {
            rank = "same"
            -"A1"
            -"A2"
            -"A3"
        }
        subgraph {
            rank = "same"
            -"B1"
            -"B2"
            -"B3"
        }
    }

    val g3 = digraph("G") {

        subgraph("cluster_0") {
            node {
                style = "filled"
                color = "white"
                "a0" - "a1"
                "a1" - "a2"
                "a2" - "a3"
            }
            style = "filled"
            color = "lightgrey"
            label = "process #1"
        }

        subgraph("cluster_1") {
            node {
                style = "filled"
            }
            "b0" - "b1"
            "b1" - "b2"
            "b2" - "b3"
            color = "blue"
            label = "process #2"
        } - "a0"

        "start" - "a0"
        "start" - "b0"
        "a1" - "b3"
        "b2" - "a3"
        "a3" - "a0"
        "a3" - "end"
        "b3" - "end"

        -"start" + {
            shape = "Mdiamond"
        }
        -"end" + {
            shape = "Msquare"
        }
    }

    // TODO fix subgraph to node connections
    // node to subgraph works
    var g4 = digraph {
        subgraph {
            node {
                color = "blue"
            }
            -"x"
            -"y"
        } - "a"
    }
    //    println(graph.dot())
    //    print("\n\n\n")
    //    println(g2.dot())
//    println(g3.dot())
    println(g4.dot())
}
