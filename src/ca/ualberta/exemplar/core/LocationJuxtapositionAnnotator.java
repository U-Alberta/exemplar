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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.ValueAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.Annotator;

public class LocationJuxtapositionAnnotator implements Annotator {
	
	private Map<String,Integer> nerPairs = new HashMap<String,Integer>();
	
	public LocationJuxtapositionAnnotator(String s, Properties p){
		
	}

	@Override
	public void annotate(Annotation document) {
		if(document.has(TokensAnnotation.class)){
			String[] ner = new String[3];
			List<CoreLabel> tokens = document.get(TokensAnnotation.class);
			CoreLabel prev = null;
			for(CoreLabel token : tokens){
				ner[0] = ner[1];
				ner[1] = ner[2];
				ner[2] = token.get(NamedEntityTagAnnotation.class);				
				
				if(ner[1] != null && !ner[1].equals("O") && ner[2] != null && !ner[2].equals("O")){
					// Two named entities in a row
				}else if(ner[0] != null && !ner[0].equals("O") && ner[2] != null && !ner[2].equals("O") && prev.get(TextAnnotation.class).equals(",")){
					//Named entity comma named entity
					String textRep = ner[0] + "," + ner[2];
					//System.out.println(textRep);
					if(nerPairs.containsKey(textRep)){
						nerPairs.put(textRep, nerPairs.get(textRep)+1);
					}else{
						nerPairs.put(textRep, 1);
					}
					//System.out.println(nerPairs);
					if (ner[0].equals("LOCATION") && ner[2].equals("LOCATION")) {
						prev.set(TextAnnotation.class, "and");
						prev.set(ValueAnnotation.class, "and");
						prev.set(PartOfSpeechAnnotation.class, "CC");
						prev.set(LemmaAnnotation.class, "and");
					}
				}
				
				prev = token;
			}
		}
	}

}
