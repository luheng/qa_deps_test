package decoding;

import java.util.Arrays;

public class AdjacencyGraph {

	public int numNodes;
	public double[][] edges;
	
	public AdjacencyGraph(int numNodes) {
		this.numNodes = numNodes;
		this.edges = new double[numNodes][numNodes];
		for (int i = 0; i < numNodes; i++) {
			Arrays.fill(edges[i], 0);
		}
	}
	
	public void setEdge(int u, int v, double weight)  {
		edges[u][v] = weight;
	}
	
	public void prettyPrint() {
		System.out.println(this.numNodes);
		for (int i = 0; i < numNodes; i++) {
			System.out.print("\t[" + i + "]");
		}
		System.out.println();
		for (int i = 0; i < numNodes; i++) {
			System.out.print("[" + i + "]");
			for (int j = 0; j < numNodes; j++) {
				System.out.print(String.format("\t%.3f", this.edges[i][j]));
			}
			System.out.println();
		}
	}

	public static AdjacencyGraph getDistanceWeightedGraph(int numNodes) {
		// 0 is reserved for root, and treated specially.
		// Each node has equal chance to have an edge from root.
		AdjacencyGraph graph = new AdjacencyGraph(numNodes + 1);
		// We do not want negative score.
		double minScore = 1e-3;
		double rootScore = 1.0 / numNodes;
		for (int i = 1; i <= numNodes; i++) {
			graph.setEdge(0, i, rootScore);
		}
		for (int i = 1; i <= numNodes; i++) {
			for (int j = 1; j <= numNodes; j++) {
				int dist = Math.abs(i - j);
				if (i < j) {
					// Right branching case.
					graph.setEdge(i, j, Math.max(minScore, 1.0 / dist - 0.1));
				} else if (i > j) {
					// Left branching case.
					graph.setEdge(i, j, Math.max(minScore, 1.0 / dist + 0.1));
				}
			}
		}
		return graph;
	}
	
	// Test graph.
	public static void main(String[] args) {
		AdjacencyGraph graph = AdjacencyGraph.getDistanceWeightedGraph(10);
		graph.prettyPrint();
	}
}
