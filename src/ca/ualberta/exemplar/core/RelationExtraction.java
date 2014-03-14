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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ca.ualberta.exemplar.util.Paths;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.semgrex.SemgrexMatcher;
import edu.stanford.nlp.semgrex.SemgrexPattern;
import edu.stanford.nlp.trees.GrammaticalRelation;
import edu.stanford.nlp.trees.semgraph.SemanticGraph;
import edu.stanford.nlp.trees.semgraph.SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation;
import edu.stanford.nlp.util.CoreMap;

public class RelationExtraction {

	private Parser parser;
	
	private SemgrexPattern patternA, patternB, patternC;
	private Map<String,String> nominalizedVerbMap;
	private boolean filterRelations;
	private ArgumentExtraction argExtractor;

	public RelationExtraction(Parser parser) throws FileNotFoundException {

		this.parser = parser;

		readNominalizedVerbMap();
		String nominalizedVerbRegex = buildNominalizedVerbRegex();
		argExtractor = new ArgumentExtraction();
		filterRelations=true;

		// Relation: verb
		patternA = SemgrexPattern
				.compile("[{tag:/VB.*/;ner:O}=rel | {tag:/NNS?/;word:/"+nominalizedVerbRegex+"/;ner:O}=relnom] " + 
						"!>/cop|appos/ {} !<nn {} !>/dobj|nsubjpass/ {tag:/NNS?/;ner:O}");
		// Relation: verb + noun
		patternB = SemgrexPattern
				.compile("{tag:/VB.*/;ner:O}=rel >/dobj|nsubjpass/ {tag:/NNS?/;ner:O}=dobj");
		
		// Relation: copula + noun
		patternC = SemgrexPattern
				.compile("{tag:/NN.*/;ner:O}=rel ?>cop {}=copula");
	}
	
	public RelationExtraction() throws FileNotFoundException{
		this(new ParserMalt());
	}

	private void readNominalizedVerbMap()
			throws FileNotFoundException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				new FileInputStream(new File(Paths.NORMALIZED_VERBS))));
		nominalizedVerbMap = new HashMap<String,String>();
		String line = null;
		try {
			while (true) {

				line = reader.readLine();
				if (line == null) {
					break;
				}
				String[] pair = line.split("\t");
				if(pair.length >= 2){
					nominalizedVerbMap.put(pair[1].trim(), pair[0].trim());
				}

			}

			reader.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	private String buildNominalizedVerbRegex(){
		StringBuilder regex = new StringBuilder();
		for(String nominalizedVerb : nominalizedVerbMap.keySet()){
			regex.append(nominalizedVerb);
			regex.append('|');
		};
		regex.deleteCharAt(regex.length()-1);
		return regex.toString();
	}
	
	public List<RelationInstance> extractNAryRelations(CoreMap sentence) {
		
		List<RelationInstance> ret = new ArrayList<RelationInstance>();
		
		SemanticGraph dependencies = sentence.get(CollapsedCCProcessedDependenciesAnnotation.class);
		if(dependencies == null){
			return ret;
		}

		/*System.out.println();
		System.out.println(dependencies.toFormattedString());

		for(IndexedWord word : dependencies.vertexSet()){
			System.out.println("Word: " + word + " -- " + word.ner() + "--" + word.beginPosition() + "--" + word.index());
		}
		
		System.out.println();
		List<CoreLabel> tokens = sentence.get(TokensAnnotation.class);
		for(CoreLabel token : tokens){
			String word = token.get(TextAnnotation.class);
			String ne = token.get(NamedEntityTagAnnotation.class);
			System.out.println(word + " -- " + ne + " -- " + token.beginPosition());
		}*/

		// Template A
		{
			SemgrexMatcher matcher = patternA.matcher(dependencies);
			while (matcher.find()) {
				IndexedWord rel = matcher.getNode("rel");
				IndexedWord relnom = matcher.getNode("relnom");
				boolean nom = relnom != null;
				if(nom)
					rel = relnom;
				if(!hasNerChild(rel, dependencies))
					continue;

				RelationInstance instance = new RelationInstance();
				List<IndexedWord> triggers = new ArrayList<IndexedWord>(2);
				
				if(nom){
					instance.setNormalizedRelation(nominalizedVerbMap.get(relnom.word()));
					instance.setOriginalRelation(relnom.word());
					triggers.add(relnom);
				}else{
					instance.setNormalizedRelation(produceRelationName(rel, null, dependencies, true));
					instance.setOriginalRelation(produceRelationName(rel, null, dependencies, false));
					triggers.add(rel);
				}
				
				instance.setTriggerIndex(triggers.get(0).index()-1);
				//System.out.println("Template A: " + relText);
				argExtractor.extractArgumentsTemplateA(sentence, dependencies, triggers, instance);
				if(shouldAddInstance(instance)){
					ret.add(instance);
				}

			}
		}

		// Template B
		{
			SemgrexMatcher matcher = patternB.matcher(dependencies);
			while (matcher.find()) {
				IndexedWord rel = matcher.getNode("rel");
				if(!hasNerChild(rel, dependencies))
					continue;
				List<IndexedWord> triggers = new ArrayList<IndexedWord>();
				triggers.add(rel);
				IndexedWord dobj = matcher.getNode("dobj");
				if(dobj != null){
					triggers.add(dobj);
				}

				RelationInstance instance = new RelationInstance();
				instance.setTriggerIndex(triggers.get(0).index()-1);
				instance.setNormalizedRelation(produceRelationName(rel, dobj, dependencies, true));
				instance.setOriginalRelation(produceRelationName(rel, dobj, dependencies, false));

				//System.out.println("Template B: " + instance.getOriginalRelation());
				argExtractor.extractArgumentsTemplateB(sentence,dependencies, triggers, instance);
				if(shouldAddInstance(instance)){
					ret.add(instance);
				}
			}
		}

		// Template C
		{
			SemgrexMatcher matcher = patternC.matcher(dependencies);
			while (matcher.find()) {
				IndexedWord copula = matcher.getNode("copula");
				IndexedWord rel = matcher.getNode("rel");
				List<IndexedWord> triggers = new ArrayList<IndexedWord>();
				triggers.add(rel);

				RelationInstance instance = new RelationInstance();
				
				if(copula != null){
					triggers.add(copula);
					instance.setNormalizedRelation(produceRelationName(copula, rel, dependencies, true));
					instance.setOriginalRelation(produceRelationName(copula, rel, dependencies, false));
				}else{
					instance.setNormalizedRelation(produceRelationName("be", rel, dependencies, true));
					instance.setOriginalRelation(produceRelationName("", rel, dependencies, false));
				}
				instance.setTriggerIndex(triggers.get(0).index()-1);
				//System.out.println("Template C: " + instance.getOriginalRelation());
				argExtractor.extractArgumentsTemplateC(sentence, dependencies, triggers, instance);
				if(shouldAddInstance(instance)){
					ret.add(instance);
				}
			}
		}
		//System.out.println(instanceList);
		return ret;

	}

	public List<RelationInstance> extractRelations(String text) {
		return extractRelations(text,true);
	}
	
	public List<RelationInstance> extractRelations(String text, boolean filterRelations) {

		List<RelationInstance> instanceList = new ArrayList<RelationInstance>();
		this.filterRelations= filterRelations;
		
		List<CoreMap> sentences = parser.parseText(text);
		
		if (sentences.size() <= 0) {
			System.err.println("No sentences found for: " + text);
			return instanceList;
		}

		for(CoreMap sentence : sentences){
			try {
				List<RelationInstance> newInstances = extractNAryRelations(sentence);

				List<CoreLabel> tokens = sentence.get(TokensAnnotation.class);
				StringBuilder sentenceString = new StringBuilder();
				for(CoreLabel token : tokens){
					String word = token.get(TextAnnotation.class);
					sentenceString.append(word + " ");
				}

				for(RelationInstance instance : newInstances){
					instance.setSentence(sentenceString.toString().trim());
					instanceList.add(instance);
				}
			}catch(Exception e){
				e.printStackTrace();
				System.out.println("Resuming...");
			}
			
		}
		
		return instanceList;

	}
	
	/*public List<RelationInstance> extractRelationsFromSentence(String text, boolean filterRelations) {

		List<RelationInstance> instanceList = new ArrayList<RelationInstance>();
		this.filterRelations= filterRelations;
		Annotation document = new Annotation(text);
		pipeline.annotate(document);

		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		if (sentences.size() <= 0) {
			System.err.println("No sentences found for: " + text);
			return instanceList;
		}

		CoreMap sentence = sentences.get(0);
		
		return extractNAryRelations(sentence);
		
	}*/
	
	/*public List<RelationInstance> extractRelationsFromSentence(String text) {
		return extractRelationsFromSentence(text, false);
	}*/

	private boolean shouldAddInstance(RelationInstance instance) {

		if(filterRelations == false){
			return true;
		}
		
		if(instance.getArguments().size() >= 2){
			for(Argument arg : instance.getArguments()){
				if(arg.getArgumentType().equals("SUBJ") || arg.getArgumentType().equals("DOBJ")){
					return true;
				}
			}
		}

		return false;
	}

	private static void sortWordsByIndex(List<IndexedWord> words){
		Collections.sort(words, new Comparator<IndexedWord>(){

			@Override
			public int compare(IndexedWord a0, IndexedWord a1) {
				return a0.index() - a1.index();
			}

		});
	}

	private static String produceRelationName(IndexedWord verb, IndexedWord noun, SemanticGraph dependencies, boolean shouldNormalize){
		StringBuilder rel = new StringBuilder();
		List<IndexedWord> verbPhrase = new ArrayList<IndexedWord>();
		List<IndexedWord> nounPhrase = new ArrayList<IndexedWord>();
		
		if(verb != null)
			verbPhrase.add(verb);
		if(noun != null)
			nounPhrase.add(noun);
		
		if(!shouldNormalize){
			if(noun != null)
				addModifiers(nounPhrase, noun, dependencies);
			if(verb != null)
				addModifiers(verbPhrase, verb, dependencies);
		}
		
		sortWordsByIndex(verbPhrase);
		sortWordsByIndex(nounPhrase);
		
		for(IndexedWord word : verbPhrase){
			if(shouldNormalize)
				rel.append(word.lemma());
			else
				rel.append(word.word());
			
			rel.append(' ');
		}
		
		for(IndexedWord word : nounPhrase){
			if(shouldNormalize)
				rel.append(word.lemma());
			else
				rel.append(word.word());
			
			rel.append(' ');
		}
		
		return rel.toString().trim();
	}
	
	private static String produceRelationName(String verb, IndexedWord noun, SemanticGraph dependencies, boolean shouldNormalize){
		StringBuilder rel = new StringBuilder();
		List<IndexedWord> nounPhrase = new ArrayList<IndexedWord>();
		rel.append(verb + ' ');
		
		if(noun != null)
			nounPhrase.add(noun);
		
		if(!shouldNormalize)
			if(noun != null)
				addModifiers(nounPhrase, noun, dependencies);
		
		sortWordsByIndex(nounPhrase);
		
		for(IndexedWord word : nounPhrase){
			if(shouldNormalize)
				rel.append(word.lemma());
			else
				rel.append(word.word());
			
			rel.append(' ');
		}
		
		return rel.toString().trim();
	}
	
	private static boolean hasNerChild(IndexedWord word, SemanticGraph dependencies){
		for(IndexedWord child : dependencies.getChildren(word)){
			if(child.ner() != null){
				return true;
			}
		}
		return false;
	}

	private static void addModifiers(List<IndexedWord> words, IndexedWord word, SemanticGraph dependencies){
		List<IndexedWord> adjs = dependencies.getChildrenWithReln(word, GrammaticalRelation.valueOf("amod"));
		List<IndexedWord> nns = dependencies.getChildrenWithReln(word, GrammaticalRelation.valueOf("nn"));
		List<IndexedWord> negs = dependencies.getChildrenWithReln(word, GrammaticalRelation.valueOf("neg"));
		List<IndexedWord> pvts = dependencies.getChildrenWithReln(word, GrammaticalRelation.valueOf("pvt")); // phrasal verb particle -- shut down
		

		List<IndexedWord> newWords = new ArrayList<IndexedWord>();
		if(adjs != null) newWords.addAll(adjs);
		if(nns != null) newWords.addAll(nns);
		if(negs != null) newWords.addAll(negs);
		if(pvts != null) newWords.addAll(pvts);

		for(IndexedWord newWord : newWords){
			
			if(Math.abs(word.index() - newWord.index()) > 5){
				// If a modifier is too far way from trigger (> 5 tokens), ignore this modifier since it is probably a mistake
				continue;
			}
			
			if(!newWord.ner().equals("PERSON") && !newWord.ner().equals("ORGANIZATION") && !newWord.ner().equals("LOCATION") && !newWord.ner().equals("MISC")){
				words.add(newWord);
			}
		}

	}
	


}
