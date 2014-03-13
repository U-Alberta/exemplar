/*
 * Copyright 2014 Filipe Mesquita and Jordan Schmidek
 * 
 * This file is part of EXEMPLAR.

 * EXEMPLAR is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * EXEMPLAR is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with EXEMPLAR.  If not, see <http://www.gnu.org/licenses/>.
 */
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
