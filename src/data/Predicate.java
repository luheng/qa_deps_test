package data;

import java.util.ArrayList;
import java.util.Arrays;

public class Predicate {
	public SRLSentence sentence;
	public int predID, predType;
	// In argIDs, -1 denotes this argument is not present
	// (i.e. only has A2, A3).
	public int[] argIDs;
	public ArrayList<Integer> argModIDs, argModTypes;
	
	private static final int maxNumArguments = 6; 
	
	public Predicate() {
		this.predID = this.predType = -1;
		this.argIDs = new int[maxNumArguments];
		Arrays.fill(argIDs, -1);
		this.argModIDs = new ArrayList<Integer>();
		this.argModTypes = new ArrayList<Integer>();
	}
	
	public void setPredicate(int predID, int predType) {
		this.predID = predID;
		this.predType = predType;
	}
	
	public void addArgument(int argID, int argNum) {
		this.argIDs[argNum] = argID;
	}
	
	public void addArgumentModifier(int argModID, int argModType) {
		this.argModIDs.add(argModID);
		this.argModTypes.add(argModType);
	}
	
	public String toString() {
		return "todo";
	}
}
