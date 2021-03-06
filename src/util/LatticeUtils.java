package util;

import java.util.Arrays;

public class LatticeUtils {

	public static void fill(int[][] arr, int val) {
		for (int i = 0; i < arr.length; i++) {
			Arrays.fill(arr[i], val);
		}
	}
	
	public static void fill(int[][][] arr, int val) {
		for (int i = 0; i < arr.length; i++) {
			fill(arr[i], val);
		}
	}
	
	public static void fill(double[][] arr, double val) {
		for (int i = 0; i < arr.length; i++) {
			Arrays.fill(arr[i], val);
		}
	}
	
	public static void fill(double[][][] arr, double val) {
		for (int i = 0; i < arr.length; i++) {
			fill(arr[i], val);
		}
	}
	
	public static void fill(String[][] arr, String val) {
		for (int i = 0; i< arr.length; i++) {
			for (int j = 0; j < arr[i].length; j++) {
				arr[i][j] = val;
			}
		}
	}
	
	public static void copy(int[] dest, int[] src) {
		for (int i = 0; i < src.length; i++) {
			dest[i] = src[i];
		}
	}
	
	public static void copy(int[][] dest, int[][] src) {
		for (int i = 0; i < src.length; i++) {
			for (int j = 0; j < src[i].length; j++)
				dest[i][j] = src[i][j];
		}
	}
	
	// TODO: can we use Object[][] for this?
	public static void copy(double[][] dest, double[][] src) {
		for (int i = 0; i < src.length; i++) {
			for (int j = 0; j < src[i].length; j++)
				dest[i][j] = src[i][j];
		}
	}
	
	public static void addTo(double[][] dest, double[][] src) {
		for (int i = 0; i < src.length; i++) {
			for (int j = 0; j < src[i].length; j++) {
				dest[i][j] += src[i][j];
			}
		}
	}
	
	public static void addTo(double[][] dest, double[][] src, double weight) {
		for (int i = 0; i < src.length; i++) {
			for (int j = 0; j < src[i].length; j++) {
				dest[i][j] += src[i][j] * weight;
			}
		}
	}

	public static double L2NormSquared(double[] arr) {
		double norm = .0;
		for (double a : arr) {
			norm += a * a;
		}
		return norm;
	}
}
