package util;

import gnu.trove.list.array.TIntArrayList;

import java.util.Arrays;
import java.util.Random;

public class RandomSampler {
	// Return an integer array of 0s and 1s.
	public static int[] sampleIDs(int numInstances, int numToSample,
								  int randomSeed) {
		boolean[] sampled = new boolean[numInstances];
		TIntArrayList samples = new TIntArrayList();
		Arrays.fill(sampled, false);
		Random random = new Random(randomSeed);
		for (int i = 0; i < numToSample; i++) {
			int sampleID = -1;
			do {
				sampleID = random.nextInt(numInstances);
			} while (sampled[sampleID]);
			sampled[sampleID] = true;
			samples.add(sampleID);
		}
		return samples.toArray();
	}
}
