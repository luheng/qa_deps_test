package util;

import gnu.trove.list.array.TIntArrayList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;

public class RandomSampler {
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
	
	public static int[] sampleIDs(int[] ids, int numInstances, int numToSample,
			  int randomSeed) {
		assert (numInstances <= ids.length);
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
			samples.add(ids[sampleID]);
		}
		return samples.toArray();
	}
	
	// This is useless.
	public static int[] shuffleIDs(int numInstances, int randomSeed) {
		ArrayList<Integer> ids = new ArrayList<Integer>();
		for (int i = 0; i < numInstances; i++) {
			ids.add(i);
		}
		Collections.shuffle(ids, new Random(randomSeed));
		int[] shuffledIds = new int[numInstances];
		for (int i = 0; i < numInstances; i++) {
			shuffledIds[i] = ids.get(i);
		}
		return shuffledIds;
	}
}
