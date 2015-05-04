package learning;

import java.util.Arrays;

import gnu.trove.map.hash.TIntDoubleHashMap;

public class FeatureVector {
	public int[] ids;
	public double[] vals;
	public int length;
	
	public FeatureVector(int[] ids, double[] vals) {
		this.ids = ids;
		this.vals = vals;
		this.length = ids.length;
	}
	
	public FeatureVector(TIntDoubleHashMap fv) {
		int[] fids = Arrays.copyOf(fv.keys(), fv.size());
		Arrays.sort(fids);
		length = fids.length;
		ids = new int[length];
		vals = new double[length];
		for (int i = 0; i < length; i++) {
			ids[i] = fids[i];
			vals[i] = fv.get(fids[i]);
		}
	}
}
