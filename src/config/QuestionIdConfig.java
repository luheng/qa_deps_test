package config;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import util.StrUtils;

public class QuestionIdConfig {
	static Properties properties;
	public String[] trainSets, testSets;
	public String featureOutputPath;
	public int randomSeed;
	public int minFeatureFreq;
	public int minQuestionLabelFreq;
	public int kBest;
	public int featureKBest;
	public boolean normalizeFeatures;
	
	public boolean regenerateSamples;
	public boolean useLexicalFeatures;
	public boolean useDependencyFeatures;
	public boolean use1BestFeatures;
	
	public boolean aggregateLabels;
	public int numPRCurvePoints;
	public double evalThreshold;
	public int evalTopK;
	
	public String[] liblinParameters;
	
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
		// cvFolds = Integer.parseInt(properties.getProperty("cvFolds"));
		minFeatureFreq = Integer.parseInt(properties.getProperty("minFeatureFreq"));
		minQuestionLabelFreq = Integer.parseInt(properties.getProperty("minQuestionLabelFreq"));
		kBest = Integer.parseInt(properties.getProperty("kBest"));
		featureKBest = Integer.parseInt(properties.getProperty("featureKBest"));
		normalizeFeatures = Boolean.parseBoolean(properties.getProperty("normalizeFeatures"));
		
		evalThreshold = Double.parseDouble(properties.getProperty("evalThreshold"));
		evalTopK = Integer.parseInt(properties.getProperty("evalTopK"));
		numPRCurvePoints = Integer.parseInt(properties.getProperty("numPRCurvePoints"));
		
		regenerateSamples = Boolean.parseBoolean(properties.getProperty("regenerateSamples"));
		useLexicalFeatures = Boolean.parseBoolean(properties.getProperty("useLexicalFeatures"));
		useDependencyFeatures = Boolean.parseBoolean(properties.getProperty("useDependencyFeatures"));
		use1BestFeatures = Boolean.parseBoolean(properties.getProperty("use1BestFeatures"));
		aggregateLabels = Boolean.parseBoolean(properties.getProperty("aggregateLabels"));
		
		trainSets = properties.getProperty("trainSets").split(",");
		testSets = properties.getProperty("testSets").split(",");
		liblinParameters = properties.getProperty("liblinParameters").split(";");
	}
	
	public String toString() {
		String str = "";
		str += "featureOutputPath\t" + featureOutputPath + "\n";
		str += "randomSeed\t" + randomSeed + "\n";
		str += "minFeatureFreq\t" + minFeatureFreq + "\n";
		
		str += "trainSets\t" + StrUtils.join(",", trainSets) + "\n";
		str += "testSets\t" + StrUtils.join(",", testSets) + "\n";
		str += "minQuestionLabelFreq\t" + minQuestionLabelFreq + "\n";
		str += "kBest\t" + kBest + "\n";
		str += "featureKBest\t" + featureKBest + "\n";
		str += "normalizeFeatures\t" + normalizeFeatures + "\n";
					
		str += "regenerateSamples\t" + regenerateSamples + "\n";
		
		str += "useLexicalFeatures\t" + useLexicalFeatures + "\n";
		str += "useDependencyFeatures\t" + useDependencyFeatures + "\n";
		str += "use1BestFeatures\t" + use1BestFeatures + "\n";
		
		str += "aggregateLabels\t" + useDependencyFeatures + "\n";
		str += "numPRCurvePoints\t" + numPRCurvePoints + "\n";
		str += "evalThreshold\t" + evalThreshold + "\n";
		str += "evalTopK\t" + evalTopK + "\n";
		return str;
	}
}
