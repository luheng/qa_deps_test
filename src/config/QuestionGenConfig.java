package config;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class QuestionGenConfig {
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
	
	public QuestionGenConfig() {
		this("answerIdConfig.properties");
	}
	
	public QuestionGenConfig(String configPath) {
		properties = new Properties();
		FileInputStream in = null;
		try {
			in = new FileInputStream(configPath);
			properties.load(in);
			in.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		featureOutputPath = properties.getProperty("featureOutptuPath");
		randomSeed = Integer.parseInt(properties.getProperty("randomSeed"));
		cvFolds = Integer.parseInt(properties.getProperty("cvFolds"));
		minFeatureFreq = Integer.parseInt(properties.getProperty("minFeatureFreq"));
		kBest = Integer.parseInt(properties.getProperty("kBest"));
		featureKBest = Integer.parseInt(properties.getProperty("featureKBest"));
		
		regenerateSamples = Boolean.parseBoolean("regenerateSamples");
		trainWithWiki = Boolean.parseBoolean("trainWithWiki");
		useSpanBasedSamples = Boolean.parseBoolean("useSpanBasedSamples");
		useLexicalFeatures = Boolean.parseBoolean("useLexicalFeatures");
		useDependencyFeatures = Boolean.parseBoolean("useDependencyFeatures");
	}

}
