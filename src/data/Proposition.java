package data;

import java.util.ArrayList;
import java.util.Arrays;

public class Proposition {
	public SRLSentence sentence;
	public int propID, propType;
	// The span of the entire group of words. For example: "has considered",
	// instead of "considered". Not sure we will need this eventually, but it
	// might help with annotation.
	public int[] span;
	// In argIDs, -1 denotes this argument is not present
	// (i.e. only has A2, A3).
	//public int[] argIDs;
	public ArrayList<Integer> argModIDs, argModTypes;
	private int numArgs;
	
	//private static final int maxNumArguments = 6; 
	
	public Proposition() {
		this.propID = this.propType = -1;
		this.span = new int[2];
		this.span[0] = this.span[1];
		//this.argIDs = new int[maxNumArguments];
		//Arrays.fill(argIDs, -1);
		this.argModIDs = new ArrayList<Integer>();
		this.argModTypes = new ArrayList<Integer>();
		this.numArgs = 0;
	}
	
	public void setPropositionSpan(int spanStart, int spanEnd) {
		this.span[0] = spanStart;
		this.span[1] = spanEnd;
	}
	
	public void setProposition(int propID, int propType) {
		this.propID = propID;
		this.propType = propType;
		this.span[0] = this.propID;
		this.span[1] = this.propID + 1;
	}
	
	/*
	public void addArgument(int argID, int argNum) {
		if (this.argIDs[argNum] == -1) {
			++numArgs;
		} else {
			System.out.println("Warning: overriding argument slot!" + this.argIDs[argNum] + " " + argID);
		}
		this.argIDs[argNum] = argID;
	}
	*/
	
	public void addArgumentModifier(int argModID, int argModType) {
		++ numArgs;
		this.argModIDs.add(argModID);
		this.argModTypes.add(argModType);
	}
	
	public int getNumArguments() {
		return numArgs;
	}
	
	@Override
	public Proposition clone() {
		Proposition newProp = new Proposition();
		newProp.propID = this.propID;
		newProp.propType = this.propType;
		newProp.span[0] = this.span[0];
		newProp.span[1] = this.span[1];
		//newProp.argIDs = Arrays.copyOf(this.argIDs, this.argIDs.length);
		for (int i = 0; i < this.argModIDs.size(); i++) {
			newProp.argModIDs.add(this.argModIDs.get(i));
			newProp.argModTypes.add(this.argModTypes.get(i));
		}
		newProp.numArgs = this.numArgs;
		return newProp;
	}
	
	// Example:
	//   verrrb \t verrrrb.01
	//   A0 \t I
	//   A1 \t cat
	//   ....
	@Override
	public String toString() {
		SRLCorpus corpus = (SRLCorpus) sentence.corpus;
		String str = sentence.getTokenString(propID) + "\t" +
					 corpus.propDict.index2str.get(propType) + "\n";
		/*
		for (int i = 0; i < maxNumArguments; i++) {
			if (argIDs[i] >= 0) {
				str += "A" + i + "\t" +
					   sentence.getTokenString(argIDs[i]) + "\n"; 
			}
		}
		*/
		for (int i = 0; i < argModIDs.size(); i++) {
			str += corpus.argModDict.index2str.get(argModTypes.get(i)) + "\t" +
				   sentence.getTokenString(argModIDs.get(i)) + "\n";
		}
		return str;
	}
	
}
