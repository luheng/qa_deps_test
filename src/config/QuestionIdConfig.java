package config;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class QuestionIdConfig {
	static Properties properties;
	public String featureOutputPath;
	public int randomSeed;
	public int cvFolds;
	public int minFeatureFreq;
	public int kBest;
	public int featureKBest;
				
	public boolean regenerateSamples;
	public boolean trainWithWiki;
	public boolean useSpanBasedSamples;
	public boolean useLexicalFeatures;
	public boolean useDependencyFeatures;
	public double evalThreshold;
	
	public QuestionIdConfig() {
		this("questionIdConfig.properties");
	}
	
	public QuestionIdConfig(String configPath) {
		properties = new Properties();
		FileInputStream in = null;
		try {
			in = new FileInputStream(configPath);
			properties.load(in);
			in.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		featureOutputPath = properties.getProperty("featureOutputPath");
		randomSeed = Integer.parseInt(properties.getProperty("randomSeed"));
		cvFolds = Integer.parseInt(properties.getProperty("cvFolds"));
		minFeatureFreq = Integer.parseInt(properties.getProperty("minFeatureFreq"));
		kBest = Integer.parseInt(properties.getProperty("kBest"));
		featureKBest = Integer.parseInt(properties.getProperty("featureKBest"));
		evalThreshold = Double.parseDouble(properties.getProperty("evalThreshold"));
		
		regenerateSamples = Boolean.parseBoolean(properties.getProperty("regenerateSamples"));
		trainWithWiki = Boolean.parseBoolean(properties.getProperty("trainWithWiki"));
		useSpanBasedSamples = Boolean.parseBoolean(properties.getProperty("useSpanBasedSamples"));
		useLexicalFeatures = Boolean.parseBoolean(properties.getProperty("useLexicalFeatures"));
		useDependencyFeatures = Boolean.parseBoolean(properties.getProperty("useDependencyFeatures"));
	}
	
	public String toString() {
		// TODO
		return "";
	}
}
