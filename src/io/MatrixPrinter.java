package io;

import data.CountDictionary;

public class MatrixPrinter {

	
	public static void prettyPrint(int[][] matrix, CountDictionary rowDict,
			CountDictionary colDict) {
		System.out.print(" ");
		for (int i = 0; i < colDict.size(); i++) {
			System.out.print("\t" + colDict.getString(i));
		}
		System.out.println();
		for (int i = 0; i < rowDict.size(); i++) {
			System.out.print(rowDict.getString(i));
			for (int j = 0; j < colDict.size(); j++) {
				System.out.print("\t" + matrix[i][j]);
			}
			System.out.println();
		}
	}
}
