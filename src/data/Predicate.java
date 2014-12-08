package data;

public class Predicate {
	SRLSentence sentence;
	int predicateID;
	// In argIDs, -1 denotes this argument is not present
	// (i.e. only has A2, A3).
	int[] argIDs, argModIDs, argModTypes;
	
	public Predicate(SRLSentence sentence, int predicateID, int[] argIDs,
			  		 int[] argModIDs, int[] argModTypes) {
		this.sentence = sentence;
		this.predicateID = predicateID;
		this.argIDs = argIDs;
		this.argModIDs = argModIDs;
		this.argModTypes = argModTypes;
	}
	
	public String toString() {
		return "todo";
	}
}
