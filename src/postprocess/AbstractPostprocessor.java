package postprocess;

import data.DepSentence;

public interface AbstractPostprocessor {
	public abstract void postprocess(int[] newParents, int[] parents,
			DepSentence sentence);
}
