package config;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class DataConfig {
	static Properties properties = null;
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
	
	public static void main(String[] args) {
	
	}
	
}
