package data;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;

public class UniversalPostagMap {
	public HashMap<String, String> tmap;
	
	public UniversalPostagMap() {
		tmap = new HashMap<String, String>();
	}
	
	public void loadFromFile(String filename) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				new FileInputStream(filename)));
		String currLine;
		while ((currLine = reader.readLine()) != null) {
			String[] info = currLine.trim().split("\\s+");
			if (info.length == 2) {
				tmap.put(info[0], info[1]);
			}
		}
		reader.close();
	}
	
	public String getUnivPostag(String tag) {
		return tmap.containsKey(tag) ? tmap.get(tag) : "X";
	}
}
