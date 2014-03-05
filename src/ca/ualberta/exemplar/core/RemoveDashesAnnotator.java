package ca.ualberta.exemplar.core;
import java.util.List;
import java.util.Properties;

import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.ValueAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.Annotator;
import edu.stanford.nlp.util.CoreMap;

public class RemoveDashesAnnotator implements Annotator {
	
	public RemoveDashesAnnotator(String s, Properties p){
		
	}

	@Override
	public void annotate(Annotation document) {
		if (document.has(SentencesAnnotation.class)) {
			for (CoreMap sentence : document.get(SentencesAnnotation.class)) {
				
				List<CoreLabel> tokens = sentence.get(TokensAnnotation.class);
				for(CoreLabel token : tokens){
					if(token.get(TextAnnotation.class).equals("--")){
						token.set(TextAnnotation.class, ",");
						token.set(ValueAnnotation.class, ",");
					}
				}
			}
		}
	}

}
