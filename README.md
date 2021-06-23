# Dotlin - Kotlin Graphviz Dot DSL

---

## About

This library is a Kotlin DSL for the Graphviz Dot language. Its syntax is fairly similar to standard Dot, with some
tweaks to get it to work as a Kotlin DSL. The library can be useful to generate graphs.

```
digraph {

    // Normal Kotlin statements
    val p = (0..9).partition { it % 2 == 0 }
    val evens = p.first
    val odds = p.second

    // Building a Dot graph
    for (i in evens) {
        +"$i" + { shape = DotNodeShape.SQUARE }
        odds.filter { abs(it - i) == 1 }.forEach { "$it" - "$i" }
    }
    for (i in odds) {
        +"$i" + { shape = DotNodeShape.CIRCLE }
        evens.filter { abs(it - i) == 1 }.forEach { "$it" - "$i" }
    }
}
```

## Puzzle
```
graph {
    "a" - "b"
    "a" - "c"
    "a" - "d"
    "b" - "a"
    "b" - "d"
    "c" - "a"
    "c" - "d"
}
```

## Example

### Dot

```
digraph g {
	node [shape=plaintext];
	A1 -> B1;
	A2 -> B2;
	A3 -> B3;
	
	A1 -> A2 [label=f];
	A2 -> A3 [label=g];
	B2 -> B3 [label="g'"];
	B1 -> B3 [label="(g o f)'" tailport=s headport=s];

	{ rank=same; A1 A2 A3 }
	{ rank=same; B1 B2 B3 } 
}
```

### Dotlin

```
digraph("g") {

    // Global node attributes
    node {
        shape = DotNodeShape.PLAINTEXT
    }
    
    // Edges
    "A1" - "B1"
    "A2" - "B2"
    "A3" - "B3"
    "A1" - "A2" + { label = "f" }
    "A2" - "A3" + { label = "g" }
    "B2" - "B3" + { label = "g'" }
    "B1" - "B3" + {
        label = "(g o f)'"
        tailport = DotPortPos.S
        headport = DotPortPos.S
    }

    +subgraph {
        rank = "same"
        +"A1"
        +"A2"
        +"A3"
    }

    +subgraph {
        rank = "same"
        +"B1"
        +"B2"
        +"B3"
    }
}
```

## Usage

### Basics

```
// This produces the Dreampuf default graph
// https://dreampuf.github.io/GraphvizOnline 

val g = digraph("G") {

    +subgraph("cluster_0") {
        node {
            style = "filled"
            color = "white"
            "a0" - "a1"
            "a1" - "a2"
            "a2" - "a3"
        }
        style = DotSubgraphStyle.FILLED
        color = "lightgrey"
        label = "process #1"
    }

    +subgraph("cluster_1") {
        node {
            style = "filled"
        }
        "b0" - "b1"
        "b1" - "b2"
        "b2" - "b3"
        color = "blue"
        label = "process #2"
    }

    "start" - "a0"
    "start" - "b0"
    "a1" - "b3"
    "b2" - "a3"
    "a3" - "a0"
    "a3" - "end"
    "b3" - "end"

    +"start" + {
        shape = DotNodeShape.DIAMOND
    }
    +"end" + {
        shape = DotNodeShape.MSQUARE
    }
}

println(g.dot())
```

### Attributes

This library has all attributes listed in the Graphviz docs with corresponding types. Some attributes allow for multiple
types such as Boolean and String so you will find attributes with the suffixes "B" and "S" for Boolean and String
respectively.

See https://graphviz.org/doc/info/attrs.html

### Graphs

```
graph { ... }

digraph { ... }

graph("name") { ... }

digraph("name") { ... }
```

### Nodes

Add nodes in graphs using the `+` unary operator like the Kotlin HTML DSL.

```
graph {
   // basic nodes
   +"a"
   +"b"
   
   // add node attributes using + binary infix op
   +"c" + {
     color = "blue"
   }
}
```

### Edges

You can add edges between nodes using the binary infix `-`. You can add edges between nodes and subgraphs. The graph
type -- graph vs digraph -- determines the edge type. In Dotlin, you just use `-` not "--" and "->".

```
graph {
    // Nodes with attributes
    +"a" + { color = "blue" }
    +"b" + { shape = DotNodeShape.TRAPEZIUM }
    
    // Basic Edge
    "a" - "b"
}

digraph {
    // Edge with attributes
    "a" - "z" + {
        color = "green"
        style = DotEdgeStyle.DASHED
    }
}
```

## Subgraphs
Add subgraphs with `+subgraph { ... }` and the code within the subgraph block is the same as for a graph/digraph block.
```
graph {

    // Add subgraph to graph
    +subgraph {
        ...
    }
    
    // Subgraph as variable
    val sg = subgraph { ... }

    // Add it to the graph
    +sg
    
    // Subgraph as edge target
    "A" - subgraph { ... }
    
    // Subgraph as edge source
    subgraph { ... } - "B"
}
```

## Reference

The library is based on the BNF given in the official docs. See https://graphviz.org/doc/info/lang.html

```
graph 	        : [ strict ] (graph | digraph) [ ID ] '{' stmt_list '}'
stmt_list       : [ stmt [ ';' ] stmt_list ]
stmt            : node_stmt
                  | 	edge_stmt
                  | 	attr_stmt
                  | 	ID '=' ID
                  | 	subgraph
attr_stmt 	: 	(graph | node | edge) attr_list
attr_list 	: 	'[' [ a_list ] ']' [ attr_list ]
a_list 	: 	ID '=' ID [ (';' | ',') ] [ a_list ]
edge_stmt 	: 	(node_id | subgraph) edgeRHS [ attr_list ]
edgeRHS 	: 	edgeop (node_id | subgraph) [ edgeRHS ]
node_stmt 	: 	node_id [ attr_list ]
node_id 	: 	ID [ port ]
port            : ':' ID [ ':' compass_pt ]
	            | 	':' compass_pt
subgraph 	: 	[ subgraph [ ID ] ] '{' stmt_list '}'
compass_pt 	: 	(n | ne | e | se | s | sw | w | nw | c | _)
```