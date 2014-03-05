package ca.ualberta.exemplar.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.trees.semgraph.SemanticGraph;
import edu.stanford.nlp.trees.semgraph.SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation;
import edu.stanford.nlp.util.CoreMap;

public class ParserStanford implements Parser {
	
	private StanfordCoreNLP pipeline;
	
	public ParserStanford(){
		Properties props = new Properties();
		props.put("customAnnotatorClass.cleanprefix", "ca.ualberta.complexre.core.CleanPrefixAnnotator");
		props.put("customAnnotatorClass.nerhack", "ca.ualberta.complexre.core.NerHackAnnotator");
		props.put("customAnnotatorClass.removedashes", "ca.ualberta.complexre.core.RemoveDashesAnnotator");
		props.put("annotators", "tokenize, ssplit, removedashes, pos, lemma, ner, cleanprefix, nerhack, parse");
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
