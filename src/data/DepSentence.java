package data;

import org.json.JSONException;
import org.json.JSONObject;

import util.StringUtils;

public class DepSentence {
	public int[] tokens, postags, parents, deptags;
	public int length;
	public DepCorpus corpus;
	public int sentenceID;
	
	public DepSentence(int[] tokens, int[] postags, int[] parents,
					   int[] deptags, DepCorpus corpus, int sentenceID) {
		this.tokens = tokens;
		this.postags = postags;
		this.parents = parents;
		this.deptags = deptags;
		this.length = tokens.length;
		this.corpus = corpus;
		this.sentenceID = sentenceID;
	}
	
	public String getTokenString(int index) {
		return corpus.wordDict.getString(tokens[index]);
	}
	
	public String getTokenString(int[] span) {
		String str = "";
		for (int i = span[0]; i < span[1]; i++) {
			if (i > span[0]) {
				str += " ";
			}
			str += getTokenString(i);
		}
		return str;
	}
	
	public String getPostagString(int index) {
		return corpus.posDict.getString(postags[index]);
	}
	
	public String getDeptagString(int index) {
		return corpus.depDict.getString(deptags[index]);
	}
	
	public String getTokensString() {
		return StringUtils.join(" ", corpus.wordDict.getStringArray(tokens));
	}
	
	public String getPostagsString() {
		return StringUtils.join(" ", corpus.posDict.getStringArray(postags));
	}
	
	public String getDeptagString() {
		return StringUtils.join(" ", corpus.depDict.getStringArray(deptags));
	}
	
	@Override
	public String toString() {
		return "ID:\t" + this.sentenceID + "\n" +
				this.getTokensString() + "\n" +
				this.getPostagsString() + "\n" +
				this.getDeptagString();
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
