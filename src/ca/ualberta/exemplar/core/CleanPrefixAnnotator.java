package ca.ualberta.exemplar.core;

import java.util.List;
import java.util.Properties;

import edu.stanford.nlp.ling.CoreAnnotations.BeforeAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.Annotator;
import edu.stanford.nlp.util.CoreMap;

public class CleanPrefixAnnotator implements Annotator {
	
	public CleanPrefixAnnotator(String s, Properties p){
		
	}

	@Override
	public void annotate(Annotation document) {
		if (document.has(SentencesAnnotation.class)) {
			for (CoreMap sentence : document.get(SentencesAnnotation.class)) {
				
				List<CoreLabel> tokens = sentence.get(TokensAnnotation.class);
				int numTokens = 0, numPrefixParts = 0;
				
				// Assumption: prefix is at max 10 tokens
				for (int i = 0; i < Math.min(tokens.size(), 10); i++) {
					
					CoreLabel token = tokens.get(i);
					String tokenText = token.get(TextAnnotation.class);
					
					if (tokenText != null
							&& numTokens > 0
							&& (tokenText.equals("--") || tokenText.equals(":"))) {
						// Assumption: if more than half the tokens are a date/location/number it's a prefix
						double fraction = (double) numPrefixParts
								/ (double) numTokens;
						if (fraction > 0.5) {
							CoreLabel nextToken = tokens.get(i + 1);
							String before = document.get(TextAnnotation.class)
									.substring(0, nextToken.beginPosition());
							nextToken.set(BeforeAnnotation.class, before);
							sentence.set(TokensAnnotation.class,
									tokens.subList(i + 1, tokens.size()));
							System.out.println("Removed Prefix: " + before);
						}
						break;
					}
					
					numTokens++;
					String neTag = token.ner();
					if (neTag != null
							&& (neTag.equals("DATE")
									|| neTag.equals("LOCATION")
									|| neTag.equals("NUMBER") || neTag
										.equals("ORDINAL"))){
						numPrefixParts++;
					}
				}
			}
		}
	}

}
