package config;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Properties;

public class DataConfig {
	static Properties properties = null;
	
	static HashMap<String, String> datasetMap;
	static {
		datasetMap = new HashMap<String, String>();
		datasetMap.put("prop-train", "propbankQATrainFilename");
		datasetMap.put("prop-dev", "propbankQADevFilename");
		datasetMap.put("prop-test", "propbankQATestFilename");
		datasetMap.put("wiki1-train", "wiki1QATrainFilename");
		datasetMap.put("wiki1-dev", "wiki1QADevFilename");
		datasetMap.put("wiki1-test", "wiki1QATestFilename");
	}
	
	static {
		properties = new Properties();
		FileInputStream in;
		try {
			in = new FileInputStream("dataConfig.properties");
			properties.load(in);
			in.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static String get(String key) {
		return properties.getProperty(key).trim();
	}
	
	/**
	 * 
	 * @param key, i.e. "prop-train"
	 * @return dataPath
	 */
	public static String getDataset(String dataset) {
		return properties.getProperty(datasetMap.get(dataset)).trim();
	}
	
	public static void main(String[] args) {
	
	}
	
}
