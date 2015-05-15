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
	
	// Example: L1R_LR,10.0,1e-2
	public LiblinearHyperParameters(String prmStr) {
		String[] prmInfo = prmStr.trim().split(",");
		this.solverType = SolverType.valueOf(prmInfo[0]);
		this.C = Double.parseDouble(prmInfo[1]);
		this.eps = Double.parseDouble(prmInfo[2]);
	}

	public String toString() {
		return "solver=" + solverType + "_C=" + C + "_eps=" + eps;
	}
}
