package config;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class ExperimentDataConfig {

	public static Properties properties = null;
	
	
	public static void config() throws IOException {
		properties = new Properties();
		FileInputStream in = new FileInputStream("experiments_config.properties");
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
		return properties.getProperty(key);
	}
	
	public static void main(String[] args) {
		try {
			ExperimentDataConfig.config();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}
