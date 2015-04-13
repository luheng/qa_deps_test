package data;

import org.json.JSONException;
import org.json.JSONObject;

import util.StringUtils;

public class DepSentence extends Sentence {
	public int[] postags, parents, deptags;
	public DepCorpus corpus;
	
	public DepSentence(int[] tokens, int[] postags, int[] parents,
					   int[] deptags, DepCorpus corpus, int sentenceID) {
		super(tokens, corpus, sentenceID);
		this.postags = postags;
		this.parents = parents;
		this.deptags = deptags;
	}
	
	public String getPostagString(int index) {
		return corpus.posDict.getString(postags[index]);
	}
	
	public String getDeptagString(int index) {
		return corpus.depDict.getString(deptags[index]);
	}
	
	public String getPostagsString() {
		return StringUtils.join(" ", corpus.posDict.getStringArray(postags));
	}
	
	public String getDeptagString() {
		return StringUtils.join(" ", corpus.depDict.getStringArray(deptags));
	}
	
	public JSONObject toJSON() {
		JSONObject jsonSent = new JSONObject();
		try {
			jsonSent.put("sentenceID", String.format("%s_%d", corpus.corpusName,
					            				     sentenceID));
			jsonSent.put("tokens", corpus.wordDict.getStringArray(tokens));
			jsonSent.put("postags", corpus.posDict.getStringArray(postags));
			jsonSent.put("deptags", corpus.depDict.getStringArray(deptags));
			jsonSent.put("parents", parents);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return jsonSent;
	}
	
	/**
	 * Adapted from Luke's code ...
	 * @param gold
	 * @param scorer
	 * @return
	 */
	public void prettyPrintDebugString(int[] prediction, double[][] scores){
		for (int i = 0; i < length; i++) {
			int predParent = prediction[i],
				goldParent = parents[i];
			if (getPostagString(i).equals(".")) {
				System.out.print(
						String.format("%2d\t%-15s\t%-5s\t-\t-\t-\t-",
								i,
								getTokenString(i), 
								getPostagString(i)));
			} else {
				System.out.print(
						String.format("%2d\t%-15s\t%-5s\t%2d\t%2.2f\t%2d\t%2.2f",
								i,
								getTokenString(i), 
								getPostagString(i),
								predParent,
								scores[predParent + 1][i + 1],
								goldParent,
								scores[goldParent + 1][i + 1]));
				if (goldParent != predParent){
					System.out.print("\t**");
				}
			}		
			System.out.println();
		}
	}
	
	/* Used for simple arc visualizer.
	 * example:
	 * 	{ "id":1, "words":["a", "b"], "tags":["A", "B"], "gold":[0,1], "pred":[2,0] }
	 */
	public void prettyPrintJSONDebugString(int[] prediction) {
		String jsonStr = "{ \"id\":" + this.sentenceID + ",";
		jsonStr += "\n\"words\": [";
		for (int i = 0; i < length; i++) {
			if (i > 0) {
				jsonStr += ", ";
			}
			jsonStr += "\"" + this.getTokenString(i) + "\"";
		}
		jsonStr += "],\n\"tags\": [";
		for (int i = 0; i < length; i++) {
			if (i > 0) {
				jsonStr += ", ";
			}
			jsonStr += "\"" + this.getPostagString(i) + "\"";
		}
		jsonStr += "],\n\"gold\": [";
		for (int i = 0; i < length; i++) {
			if (i > 0) {
				jsonStr += ", ";
			}
			jsonStr += this.parents[i] + 1;
		}
		jsonStr += "], \n\"pred\": [";
		for (int i = 0; i < length; i++) {
			if (i > 0) {
				jsonStr += ", ";
			}
			jsonStr += prediction[i] + 1;
		}
		jsonStr += "] }";
		System.out.println(jsonStr);
	}
}
