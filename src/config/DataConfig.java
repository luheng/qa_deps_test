package config;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class DataConfig {
	public static Properties properties = null;
	
	public static void config() throws IOException {
		properties = new Properties();
		FileInputStream in = new FileInputStream("dataConfig.properties");
		properties.load(in);
		in.close();
		
		for (Object key : properties.keySet()) {
			System.out.println(key + ", " + properties.getProperty((String) key));
		}
	}

	public static String get(String key) {
		if (properties == null) {
			try {
				config();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return properties.getProperty(key).trim();
	}
	
	public static void main(String[] args) {
		try {
			DataConfig.config();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}
