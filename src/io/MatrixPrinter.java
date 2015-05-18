package io;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import data.CountDictionary;

public class MatrixPrinter {

	
	public static void prettyPrint(int[][] matrix, CountDictionary rowDict,
			CountDictionary colDict) {
		int numRows = rowDict.size(), numCols = colDict.size();
		ArrayList<String> rowLabels = new ArrayList<String>(),
					      colLabels = new ArrayList<String>();
		int[] rowSum = new int[numRows];
		int[] colSum = new int[numCols];
		Arrays.fill(rowSum, 0);
		Arrays.fill(colSum, 0);
		for (int i = 0; i < numRows; i++) {
			for (int j = 0; j < numCols; j++) {
				rowSum[i] += matrix[i][j];
				colSum[j] += matrix[i][j];
			}
		}
		for (int i = 0; i < numRows; i++) {
			rowLabels.add(rowDict.getString(i));
		}
		for (int i = 0; i < numCols; i++) {
			colLabels.add(colDict.getString(i));
		}
		Collections.sort(rowLabels);
		Collections.sort(colLabels);
		
		System.out.print(" ");
		for (String cl : colLabels) {
			int cid = colDict.lookupString(cl);
			if (colSum[cid] > 0) {
				System.out.print("\t" + cl);
			}
		}
		System.out.println();
		for (String rl : rowLabels) {
			int rid = rowDict.lookupString(rl);
			if (rowSum[rid] == 0) {
				continue;
			}
			System.out.print(rl);
			for (String cl : colLabels) {
				int cid = colDict.lookupString(cl);
				if (colSum[cid] > 0) {
					System.out.print("\t" + matrix[rid][cid]);
				}
			}
			System.out.println();
		}
	}
	
	public static void prettyPrint(int[] arr, CountDictionary rowDict) {
		int numRows = rowDict.size();
		ArrayList<String> rowLabels = new ArrayList<String>();
		for (int i = 0; i < numRows; i++) {
			rowLabels.add(rowDict.getString(i));
		}
		Collections.sort(rowLabels);
		
		for (String rl : rowLabels) {
			int rid = rowDict.lookupString(rl);
			if (arr[rid] > 0) {
				System.out.println(rl + "\t" + arr[rid]);
			}
		}
	}
	
}
