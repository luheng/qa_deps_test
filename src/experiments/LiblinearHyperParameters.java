package experiments;

import de.bwaldvogel.liblinear.SolverType;

public class LiblinearHyperParameters {
	public SolverType solvertType;
	public double C, eps;
	
	public LiblinearHyperParameters(SolverType solverType, double C, double eps) {
		this.solvertType = solverType;
		this.C = C;
		this.eps = eps;
	}
}
