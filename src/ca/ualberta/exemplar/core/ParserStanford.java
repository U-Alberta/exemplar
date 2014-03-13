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
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

public class ParserStanford implements Parser {
	
	private StanfordCoreNLP pipeline;
	
	public ParserStanford(){
		Properties props = new Properties();
		props.put("customAnnotatorClass.cleanprefix", "ca.ualberta.exemplar.core.CleanPrefixAnnotator");
		props.put("customAnnotatorClass.locationjuxtaposition", "ca.ualberta.exemplar.core.LocationJuxtapositionAnnotator");
		props.put("customAnnotatorClass.removedashes", "ca.ualberta.exemplar.core.RemoveDashesAnnotator");
		props.put("annotators", "tokenize, ssplit, removedashes, pos, lemma, ner, cleanprefix, locationjuxtaposition, parse");
		pipeline = new StanfordCoreNLP(props);
	}
	
	@Override
	public List<CoreMap> parseText(String text){
		Annotation document = new Annotation(text);
		pipeline.annotate(document);

		List<CoreMap> sentences = document.get(SentencesAnnotation.class);

		/*for(CoreMap sentence : sentences){
			SemanticGraph dependencies = sentence.get(CollapsedCCProcessedDependenciesAnnotation.class);
			graphs.add(dependencies);
		}*/
		
		return sentences;
		

	}

}
