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
	
	@Override
	public Predicate clone() {
		Predicate newPred = new Predicate();
		newPred.predID = this.predID;
		newPred.predType = this.predType;
		newPred.argIDs = Arrays.copyOf(this.argIDs, this.argIDs.length);
		for (int i = 0; i < this.argModIDs.size(); i++) {
			newPred.argModIDs.add(this.argModIDs.get(i));
			newPred.argModTypes.add(this.argModTypes.get(i));
		}
		return newPred;
	}
	
	// Example:
	//   verrrb \t verrrrb.01
	//   A0 \t I
	//   A1 \t cat
	//   ....
	@Override
	public String toString() {
		SRLCorpus corpus = (SRLCorpus) sentence.corpus;
		String str = sentence.getTokenString(predID) + "\t" +
					 corpus.predDict.index2str.get(predType) + "\n";
		for (int i = 0; i < maxNumArguments; i++) {
			if (argIDs[i] >= 0) {
				str += "A" + i + "\t" +
					   sentence.getTokenString(argIDs[i]) + "\n"; 
			}
		}
		for (int i = 0; i < argModIDs.size(); i++) {
			str += corpus.argModDict.index2str.get(argModTypes.get(i)) + "\t" +
				   sentence.getTokenString(argModIDs.get(i)) + "\n";
		}
		return str;
	}
	
}
