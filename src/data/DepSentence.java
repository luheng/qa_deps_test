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
}
