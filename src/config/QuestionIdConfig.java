package config;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import util.StringUtils;

public class QuestionIdConfig {
	static Properties properties;
	public String[] trainSets, testSets;
	public String featureOutputPath;
	public int randomSeed;
	public int cvFolds;
	public int minFeatureFreq;
	public int minQuestionLabelFreq;
	public int kBest;
	public int featureKBest;
				
	public boolean regenerateSamples;
	public boolean useSpanBasedSamples;
	public boolean useLexicalFeatures;
	public boolean useDependencyFeatures;
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
		cvFolds = Integer.parseInt(properties.getProperty("cvFolds"));
		minFeatureFreq = Integer.parseInt(properties.getProperty("minFeatureFreq"));
		minQuestionLabelFreq = Integer.parseInt(properties.getProperty("minQuestionLabelFreq"));
		kBest = Integer.parseInt(properties.getProperty("kBest"));
		featureKBest = Integer.parseInt(properties.getProperty("featureKBest"));
		evalThreshold = Double.parseDouble(properties.getProperty("evalThreshold"));
		evalTopK = Integer.parseInt(properties.getProperty("evalTopK"));
		numPRCurvePoints = Integer.parseInt(properties.getProperty("numPRCurvePoints"));
		
		regenerateSamples = Boolean.parseBoolean(properties.getProperty("regenerateSamples"));
		useSpanBasedSamples = Boolean.parseBoolean(properties.getProperty("useSpanBasedSamples"));
		useLexicalFeatures = Boolean.parseBoolean(properties.getProperty("useLexicalFeatures"));
		useDependencyFeatures = Boolean.parseBoolean(properties.getProperty("useDependencyFeatures"));
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
		
		str += "trainSets\t" + StringUtils.join(",", trainSets) + "\n";
		str += "testSets\t" + StringUtils.join(",", testSets) + "\n";
		str += "minQuestionLabelFreq\t" + minQuestionLabelFreq + "\n";
		str += "kBest\t" + kBest + "\n";
		str += "featureKBest\t" + featureKBest + "\n";
					
		str += "regenerateSamples\t" + regenerateSamples + "\n";
		str += "useSpanBasedSamples\t" + useSpanBasedSamples + "\n";
		str += "useLexicalFeatures\t" + useLexicalFeatures + "\n";
		str += "useDependencyFeatures\t" + useDependencyFeatures + "\n";
		str += "aggregateLabels\t" + useDependencyFeatures + "\n";
		str += "numPRCurvePoints\t" + numPRCurvePoints + "\n";
		str += "evalThreshold\t" + evalThreshold + "\n";
		str += "evalTopK\t" + evalThreshold + "\n";
		return str;
	}
}
