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
	
	// TODO: Pretty print graph
	public void prettyPrint() {
		
	}
	
}
