package experiments;

import de.bwaldvogel.liblinear.SolverType;

public class LiblinearHyperParameters {
	public SolverType solverType;
	public double C, eps;
	
	public LiblinearHyperParameters(SolverType solverType, double C, double eps) {
		this.solverType = solverType;
		this.C = C;
		this.eps = eps;
	}
	
	public String toString() {
		return "solver=" + solverType + "_C=" + C + "_eps=" + eps;
	}
}
