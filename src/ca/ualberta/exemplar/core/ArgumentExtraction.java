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

import java.util.ArrayList;
import java.util.List;

import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.semgrex.SemgrexMatcher;
import edu.stanford.nlp.semgrex.SemgrexPattern;
import edu.stanford.nlp.trees.semgraph.SemanticGraph;
import edu.stanford.nlp.util.CoreMap;

public class ArgumentExtraction {

	final SemgrexPattern patternA1;
	final SemgrexPattern patternA2;
	final SemgrexPattern patternB1;
	final SemgrexPattern patternB2;
	final SemgrexPattern patternC1;
	final SemgrexPattern patternC2;
	
	String NER = "ner:/PERSON|ORGANIZATION|LOCATION|MISC/";
	
	public ArgumentExtraction(){
		
		patternA1 = SemgrexPattern
				.compile("{tag:/NNS?|VB.*/}=rel >nsubj=dep {"+NER+"}=arg " +
						"| >agent=dep {"+NER+"}=arg | >poss=dep {"+NER+"}=arg" +
						"| >appos=dep {"+NER+"}=arg | >amod=dep {"+NER+"}=arg" +
						"| >nn=dep {"+NER+"}=arg | >/prep.*/=dep {"+NER+"}=arg"+
						"| >iobj=dep {"+NER+"}=arg | >nsubjpass=dep {"+NER+"}=arg"+
						"| >dobj=dep {"+NER+"}=arg");

		patternA2 = SemgrexPattern
				.compile("{tag:/NNS?|VB.*/}=rel " + 
						" <partmod=dep {"+NER+"}=arg | <rcmod=dep {"+NER+"}=arg");
		
		patternB1 = SemgrexPattern
				.compile("{tag:/NNS?|VB.*/}=rel >nsubj=dep {"+NER+"}=arg " +
						"| >agent=dep {"+NER+"}=arg | >poss=dep {"+NER+"}=arg" +
						"| >amod=dep {"+NER+"}=arg | >iobj=dep {"+NER+"}=arg" +
						"| >nn=dep {"+NER+"}=arg | >/prep.*/=dep {"+NER+"}=arg" + 
						"| >appos=dep {"+NER+"}=arg");
		
		patternB2 = SemgrexPattern
				.compile("{tag:/NNS?|VB.*/}=rel " + 
						" <partmod=dep {"+NER+"}=arg | <rcmod=dep {"+NER+"}=arg");

		
		patternC1 = SemgrexPattern
				.compile("{tag:/NNS?/}=rel >nsubj=dep {"+NER+"}=arg " + 
						"| >appos=dep {"+NER+"}=arg | >amod=dep {"+NER+"}=arg" +
						"| >nn=dep {"+NER+"}=arg | >/prep.*/=dep {"+NER+"}=arg" + 
						"| >poss=dep {"+NER+"}=arg");
		
		patternC2 = SemgrexPattern
				.compile("{tag:/NNS?/}=rel <appos=dep {"+NER+"}=arg" + 
						"| <partmod=dep {"+NER+"}=arg | <rcmod=dep {"+NER+"}=arg");

	}
	//TemplateC: copula+noun
	public void extractArgumentsTemplateC(CoreMap sentence, SemanticGraph dependencies, List<IndexedWord> relationalWords, RelationInstance instance) {

		//Dependencies where the relation is the governor
		{
			SemgrexMatcher matcher = patternC1.matcher(dependencies);
			while (matcher.find()) {
				IndexedWord rel = matcher.getNode("rel");
				IndexedWord arg = matcher.getNode("arg");
				String dep = matcher.getRelnString("dep");
				dep = ">"+dep;

				if(relationalWords.contains(rel)){
					//System.out.println("ARG: " + getOriginalText(arg) + " ("+dep+", " + arg.ner() +")");
					String argumentType = rel.tag().substring(0, 2) + dep;
					Argument argument = getEntityFromHead(arg, sentence, dependencies, argumentType);
					instance.addArgument(argument);
				}
			}
		}

		// Dependencies where the entity is the governor
		{
			SemgrexMatcher matcher = patternC2.matcher(dependencies);
			while (matcher.find()) {
				IndexedWord rel = matcher.getNode("rel");
				IndexedWord arg = matcher.getNode("arg");
				String dep = matcher.getRelnString("dep");
				dep = "<"+dep;

				if(relationalWords.contains(rel)){
					//System.out.println("ARG: " + getOriginalText(arg) + " ("+dep+", " + arg.ner() +")");
					String argumentType = rel.tag().substring(0, 2) + dep;
					Argument argument = getEntityFromHead(arg, sentence, dependencies, argumentType);
					instance.addArgument(argument);
				}
			}
		}
		
		assignArgumentTypesTemplateC(instance);
	}

	//Template B: verb+noun
	public void extractArgumentsTemplateB(CoreMap sentence, SemanticGraph dependencies, List<IndexedWord> relationalWords, RelationInstance instance) {


		//Dependencies where the relation is the governor
		{
			SemgrexMatcher matcher = patternB1.matcher(dependencies);
			while (matcher.find()) {
				IndexedWord rel = matcher.getNode("rel");
				IndexedWord arg = matcher.getNode("arg");
				String dep = matcher.getRelnString("dep");
				dep = ">"+dep;

				if(relationalWords.contains(rel)){
					//System.out.println("ARG: " + getOriginalText(arg) + " ("+dep+", " + arg.ner() +")");
					String argumentType = rel.tag().substring(0, 2) + dep;
					Argument argument = getEntityFromHead(arg, sentence, dependencies, argumentType);
					instance.addArgument(argument);
				}
			}
		}

		// Dependencies where the entity is the governor
		{
			SemgrexMatcher matcher = patternB2.matcher(dependencies);
			while (matcher.find()) {
				IndexedWord rel = matcher.getNode("rel");
				IndexedWord arg = matcher.getNode("arg");
				String dep = matcher.getRelnString("dep");
				dep = "<"+dep;

				if(relationalWords.contains(rel)){
					//System.out.println("ARG: " + getOriginalText(arg) + " ("+dep+", " + arg.ner() +")");
					String argumentType = rel.tag().substring(0, 2) + dep;
					Argument argument = getEntityFromHead(arg, sentence, dependencies, argumentType);
					instance.addArgument(argument);
				}
			}
		}
		
		assignArgumentTypesTemplateB(instance);

	}

	//Template A: verb
	public void extractArgumentsTemplateA(CoreMap sentence, SemanticGraph dependencies, List<IndexedWord> relationalWords, RelationInstance instance) {

		//Dependencies where the relation is the governor
		{
			SemgrexMatcher matcher = patternA1.matcher(dependencies);
			while (matcher.find()) {
				IndexedWord rel = matcher.getNode("rel");
				IndexedWord arg = matcher.getNode("arg");
				String dep = matcher.getRelnString("dep");
				dep = ">"+dep;

				if(relationalWords.contains(rel)){
					//System.out.println("ARG: " + getOriginalText(arg) + " ("+dep+", " + arg.ner() +")");
					String argumentType = rel.tag().substring(0, 2) + dep;
					Argument argument = getEntityFromHead(arg, sentence, dependencies, argumentType);
					instance.addArgument(argument);
				}
			}
		}

		// Dependencies where the entity is the governor
		{
			SemgrexMatcher matcher = patternA2.matcher(dependencies);
			while (matcher.find()) {
				IndexedWord rel = matcher.getNode("rel");
				IndexedWord arg = matcher.getNode("arg");
				String dep = matcher.getRelnString("dep");
				dep = "<"+dep;

				if(relationalWords.contains(rel)){
					//System.out.println("ARG: " + getOriginalText(arg) + " ("+dep+", " + arg.ner() +")");
					String argumentType = rel.tag().substring(0, 2) + dep;
					Argument argument = getEntityFromHead(arg, sentence, dependencies, argumentType);
					instance.addArgument(argument);
				}
			}
		}
		
		assignArgumentTypesTemplateA(instance);

	}
	
	private void assignArgumentTypesTemplateC(RelationInstance instance) {
		List<Argument> listArguments = new ArrayList<Argument>(instance.getArguments().size());
		listArguments.addAll(instance.getArguments());
		
		// Resolve the case: the [[[LOC Greenwood Heights]]] {{{section}}} of [[[LOC Brooklyn]]]
		for(Argument arg1 : listArguments){
			for(Argument arg2 : listArguments){
				if(arg1.getArgumentType().equals("NN>amod") || arg1.getArgumentType().equals("NN>nn")){
					if(arg2.getArgumentType().equals("NN>prep_of") || arg2.getArgumentType().equals("NN>poss")){
						arg1.setArgumentType("SUBJ");
						arg2.setArgumentType("POBJ-OF");
					}
				}
			}
		}
		
		for(Argument arg : listArguments){
			
			if(arg.getArgumentType().equals("POBJ-OF") || arg.getArgumentType().equals("SUBJ")){
				continue;
			}
			
			if(arg.getArgumentType().equals("NN>nsubj") || arg.getArgumentType().equals("NN>appos") || 
					arg.getArgumentType().equals("NN<appos") || arg.getArgumentType().equals("NN<rcmod") ||
					arg.getArgumentType().equals("NN<partmod") ){
				arg.setArgumentType("SUBJ");
				continue;
			}
			
			if(arg.getArgumentType().equals("NN>amod") || arg.getArgumentType().equals("NN>nn") || 
					arg.getArgumentType().equals("NN>poss")){
				arg.setArgumentType("POBJ-OF");
				continue;
			}
			
			if(arg.getArgumentType().startsWith("NN>prep_")){
				String preposition = arg.getArgumentType().substring(8);
				arg.setArgumentType("POBJ-" + preposition.toUpperCase());
				continue;
			}
			//System.out.println("***** Dependecy unmapped: " + arg.getEntityName() + " " + arg.getArgumentType() + " ****** Template C" );
			instance.removeArgument(arg);
		}
	}
	
	private void assignArgumentTypesTemplateB(RelationInstance instance) {
		List<Argument> listArguments = new ArrayList<Argument>(instance.getArguments().size());
		listArguments.addAll(instance.getArguments());
		for(Argument arg : listArguments){
			if(arg.getArgumentType().equals("VB>nsubj") || arg.getArgumentType().equals("VB>agent") ||
					arg.getArgumentType().equals("VB<partmod") || arg.getArgumentType().equals("VB<rcmod")){
				arg.setArgumentType("SUBJ");
				continue;
			}
			
			if(arg.getArgumentType().equals("NN>amod") || arg.getArgumentType().equals("NN>nn") || 
					arg.getArgumentType().equals("NN>poss")){
				arg.setArgumentType("POBJ-OF");
				continue;
			}
			
			if(arg.getArgumentType().equals("VB>iobj")){
				arg.setArgumentType("POBJ-TO");
				continue;
			}
			
			if(arg.getArgumentType().startsWith("NN>prep_") || arg.getArgumentType().startsWith("VB>prep_")){
				String preposition = arg.getArgumentType().substring(8);
				arg.setArgumentType("POBJ-" + preposition.toUpperCase());
				continue;
			}
			//System.out.println("***** Dependecy unmapped: " + arg.getEntityName() + " " + arg.getArgumentType() + " ****** Template B" );
			instance.removeArgument(arg);
		}
	}
	
	
	private void assignArgumentTypesTemplateA(RelationInstance instance) {
		List<Argument> listArguments = new ArrayList<Argument>(instance.getArguments().size());
		listArguments.addAll(instance.getArguments());
		
		// Resolve the nominalized verb with 'by' first.
		for(Argument arg1 : listArguments){
			for(Argument arg2 : listArguments){
				if(arg1.getArgumentType().equals("NN>amod") || arg1.getArgumentType().equals("NN>nn") || 
						arg1.getArgumentType().equals("NN>poss")){
					if(arg2.getArgumentType().equals("NN>prep_by")){
						arg1.setArgumentType("DOBJ");
						arg2.setArgumentType("SUBJ");
					}
				}
			}
		}
		
		// Resolve passive voice with partmod
		for(Argument arg1 : listArguments){
			for(Argument arg2 : listArguments){
				if(arg1.getArgumentType().equals("VB<partmod") || arg1.getArgumentType().equals("VB<rcmod")){
					if(arg2.getArgumentType().equals("VB>agent")){
						arg1.setArgumentType("DOBJ");
						arg2.setArgumentType("SUBJ");
					}
				}
			}
		}
		
		for(Argument arg : listArguments){
			if(arg.getArgumentType().equals("DOBJ") || arg.getArgumentType().equals("SUBJ")){
				continue;
			}
			
			if(arg.getArgumentType().equals("VB>nsubj") || arg.getArgumentType().equals("VB>agent") ||
					arg.getArgumentType().equals("VB<partmod") || arg.getArgumentType().equals("VB<rcmod") ||
					arg.getArgumentType().equals("VB>prep_by")){
				arg.setArgumentType("SUBJ");
				continue;
			}
			
			
			if(arg.getArgumentType().equals("VB>dobj") || arg.getArgumentType().equals("VB>nsubjpass")){
				arg.setArgumentType("DOBJ");
				continue;
			}
			
			if(arg.getArgumentType().equals("VB>iobj")){
				arg.setArgumentType("POBJ-TO");
				continue;
			}
			
			if(arg.getArgumentType().equals("NN>amod") || arg.getArgumentType().equals("NN>nn") || 
					arg.getArgumentType().equals("NN>poss")){
				arg.setArgumentType("SUBJ");
				continue;
			}
			
			if(arg.getArgumentType().equals("NN>prep_of") ){
				arg.setArgumentType("DOBJ");
				continue;
			}
			
			if(arg.getArgumentType().startsWith("NN>prep_") || arg.getArgumentType().startsWith("VB>prep_")){
				String preposition = arg.getArgumentType().substring(8);
				arg.setArgumentType("POBJ-" + preposition.toUpperCase());
				continue;
			}
			//System.out.println("***** Dependecy unmapped: " + arg.getEntityName() + " " + arg.getArgumentType() + " ****** Template A" );
			instance.removeArgument(arg);
		}
	}
	
	private String normalizeEntityType(String stanfordType){
		if(stanfordType.equals("PERSON")) return "PER"; 
		if(stanfordType.equals("ORGANIZATION")) return "ORG";
		if(stanfordType.equals("LOCATION")) return "LOC";
		return stanfordType;
	}
	
	/*private String getOriginalText(IndexedWord word){
		if(word.containsKey(OriginalTextAnnotation.class)){
			return word.originalText();
		}
		return word.word();
	}*/
	
	private Argument getEntityFromHead(IndexedWord head, CoreMap sentence, SemanticGraph dependencies, String argumentType){

		int startIndex = head.index() -1; //Changing from starting at 1 to starting at 0
		int endIndex = head.index() -1;
	
		List<CoreLabel> tokens = sentence.get(TokensAnnotation.class);
		
		CoreLabel token = tokens.get(startIndex);
		String ne = token.get(NamedEntityTagAnnotation.class);
		StringBuilder builder = new StringBuilder();
		builder.append(token.get(TextAnnotation.class));
		int startOffset = token.beginPosition();
		int endOffset = token.endPosition();
		
		// Look for first token of the entity.
		for(int index = startIndex-1; index >= 0; index--){
			
			token = tokens.get(index);
			String word = token.get(TextAnnotation.class);
			if(!ne.equals(token.get(NamedEntityTagAnnotation.class)))
				break;

			startIndex--;
			builder.insert(0, word + " ");
			startOffset = token.beginPosition();
		}
		
		for(int index=endIndex+1;index < tokens.size();index++){

			token = tokens.get(index);
			String word = token.get(TextAnnotation.class);
			if(!ne.equals(token.get(NamedEntityTagAnnotation.class)))
				break;

			endIndex++;
			builder.append(" " + word);
			endOffset = token.endPosition();
		}
		
		String entityName = builder.toString();
		String entityType = normalizeEntityType(ne);
		String entityId = entityName + "#" + entityType;
		Argument argument = new Argument(argumentType, entityId, entityName, entityType, startIndex, endIndex, startOffset, endOffset); 

		return argument;
	}
	
}
