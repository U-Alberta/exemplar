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

package ca.ualberta.exemplar.evaluation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import ca.ualberta.exemplar.core.Argument;
import ca.ualberta.exemplar.core.Parser;
import ca.ualberta.exemplar.core.ParserMalt;
import ca.ualberta.exemplar.core.ParserStanford;
import ca.ualberta.exemplar.core.RelationExtraction;
import ca.ualberta.exemplar.core.RelationInstance;
import edu.stanford.nlp.trees.semgraph.SemanticGraph;
import edu.stanford.nlp.trees.semgraph.SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation;
import edu.stanford.nlp.util.CoreMap;


public class BenchmarkBinary {

	private File evaluationFile;
	PrintStream ps;
	RelationExtraction relationExtraction;

	public static final String PLACEHOLDER_ENTITY1 = "Europe";
	public static final String PLACEHOLDER_ENTITY2 = "Asia";

	public BenchmarkBinary(File evaluationFile, File outputFile) throws FileNotFoundException, UnsupportedEncodingException{
		this(evaluationFile, outputFile, null);
	}

	public BenchmarkBinary(File evaluationFile, File outputFile, String parserName) throws FileNotFoundException, UnsupportedEncodingException{
		this.evaluationFile = evaluationFile;

		Parser parser;
		if("malt".equalsIgnoreCase(parserName)){
			parser = new ParserMalt();
		}else{
			parser = new ParserStanford();
		}

		relationExtraction = new RelationExtraction(parser);

		ps = new PrintStream(outputFile, "UTF-8");
		ps.println("Entity1\tRelation\tEntity2\tAnnotated Sentence");
	}

	protected void finalize() throws Throwable {
		try {
			ps.close();        // close open files
		} finally {
			super.finalize();
		}
	}

	protected void readSentencesAndRun(){

		BufferedReader reader = null;

		try {
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(evaluationFile), Charset.forName("UTF-8")));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		String line=null;
		int count=0;
		try {
			while(true){

				line = reader.readLine();
				count++;

				if(count == 1){
					// Skip header.
					continue;
				}

				if(line == null){
					break;
				}

				if(count % 1000 == 0){
					System.out.println("Sentences processed in the NLP pipeline (so far): " + count);
				}

				String[] fields = line.split("\\t");
				String entity1 = fields[0];
				String entity2 = fields[2];
				String text = fields[4];

				String[] tokens = text.split(" ");

				StringBuilder sentence = new StringBuilder();

				boolean firstEntity = true;
				boolean firstEntityOpen = false;
				boolean secondEntityOpen = false;
				for(int i=0;i < tokens.length; i++){

					if(tokens[i].indexOf("--->") == 0){
						continue;
					}

					if(tokens[i].indexOf("<---") == 0){
						continue;
					}

					if(tokens[i].indexOf("{{{") == 0){
						tokens[i] = tokens[i].replaceAll("\\{", "");
					}

					if(tokens[i].indexOf("}}}") > 0){
						tokens[i] = tokens[i].replaceAll("\\}", "");
					}

					if(tokens[i].indexOf("[[[") == 0){
						if(firstEntity){
							firstEntityOpen=true;
						}else{
							secondEntityOpen=true;
						}

						// Skip this token: [[[TYPE
						continue;

					}

					if(!firstEntityOpen && !secondEntityOpen){
						sentence.append(tokens[i] + " ");
					}

					if(tokens[i].indexOf("]]]") > 0){
						tokens[i] = tokens[i].replaceAll("\\]", "");
						if(firstEntity){
							firstEntity=false;
							firstEntityOpen=false;
							sentence.append(PLACEHOLDER_ENTITY1 + " ");
						}else{
							secondEntityOpen = false;
							sentence.append(PLACEHOLDER_ENTITY2 + " ");
						}
					}


				}

				if(firstEntity || firstEntityOpen || secondEntityOpen){
					System.out.println("Not able to find one of the entities in the sentence: " + sentence);
					System.exit(0);
				}

				// Workaround to keep tokens as in original sentence.
				String newtext = sentence.toString().trim();
				newtext = newtext.replaceAll("-LRB-", "(");
				newtext = newtext.replaceAll("-RRB-", ")");
				newtext = newtext.replaceAll("\\\\/", "/");
				newtext = newtext.replaceAll("\\. ,", "\\.,");

				evaluate(newtext, fields[4], entity1, entity2);

			}

			reader.close();

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private RelationInstance createBinaryInstance(RelationInstance instance, Argument arg1, Argument arg2){
		RelationInstance newInstance = new RelationInstance();
		String preposition = "";
		if(arg2.getArgumentType().startsWith("POBJ")){
			preposition = arg2.getArgumentType().substring(5).toLowerCase();
		}
		newInstance.setAnnotatedSentence(instance.getAnnotatedSentence());
		newInstance.setNormalizedRelation(instance.getNormalizedRelation() + " " + preposition);
		newInstance.setOriginalRelation(instance.getOriginalRelation());
		newInstance.addArgument(arg1);
		newInstance.addArgument(arg2);
		return newInstance;
	}

	// Do not allow POBJ-POBJ binary relations
	public List<RelationInstance> extractBinaryRelations(List<RelationInstance> instanceList) {
		List<RelationInstance> newInstanceList = new ArrayList<RelationInstance>();

		for(RelationInstance instance : instanceList){
			for(Argument arg1 : instance.getArguments()){
				for(Argument arg2 : instance.getArguments()){
					if(arg1 == arg2) continue;
					
					if(arg1.getArgumentType().equals("SUBJ")){
						if(arg1 == arg2) continue;
						if(arg2.getArgumentType().equals("DOBJ") || arg2.getArgumentType().startsWith("POBJ") || arg2.getArgumentType().equals("SUBJ")){
							newInstanceList.add(createBinaryInstance(instance, arg1, arg2));
						}
					}
					if(arg1.getArgumentType().equals("DOBJ")){
						if(arg1 == arg2) continue;
						if(arg2.getArgumentType().startsWith("POBJ") || arg2.getArgumentType().equals("DOBJ")){
							newInstanceList.add(createBinaryInstance(instance, arg1, arg2));
						}
					}
				}
			}
		}

		return newInstanceList;

	}

	private void evaluate(String normalizedSentence, String originalSentence, String entity1, String entity2) throws FileNotFoundException, UnsupportedEncodingException {
		List<RelationInstance> instances = null;

		try {
			instances = relationExtraction.extractRelations(normalizedSentence, true);
			if(instances.size() > 0){
				CoreMap sentence = instances.get(0).getAnnotatedSentence();
				SemanticGraph dependencies = sentence.get(CollapsedCCProcessedDependenciesAnnotation.class);
				System.out.println("========== Sentence =========");
				System.out.println(sentence.toString());
				if(dependencies != null)
					System.out.println(dependencies.toFormattedString());
				System.out.println("======= N-ary Instaces ======");
				for(RelationInstance instance : instances){
					System.out.println(instance);
				}
			}

			instances = extractBinaryRelations(instances);

		} catch(Exception e){
			e.printStackTrace();
			System.out.println("Resuming...");
		}

		boolean relationMatched = false;

		String concatenatedRelations = "";

		if(instances!=null && instances.size()>0){
			
			System.out.println("======= Binary Instaces ======");
			
			for(RelationInstance instance : instances){

				System.out.println("Instance: " + instance.getOriginalRelation());

				boolean containMention1=false;
				boolean containMention2=false;

				for(Argument arg : instance.getArguments()){
					System.out.println("\tArg: [" + arg.getEntityId() +"] - Type: " + arg.getArgumentType());
					// .endsWith() (previously .contains()) is a hack for bad annotated entities in the ground truth, such as Andre [[[Agassi]]].
					if(arg.getEntityName().endsWith(PLACEHOLDER_ENTITY1)){
						containMention1 = true;
					}

					if(arg.getEntityName().endsWith(PLACEHOLDER_ENTITY2)){
						containMention2 = true;
					}
				}


				if(containMention1 && containMention2){
					if(concatenatedRelations.isEmpty()){
						concatenatedRelations = instance.getOriginalRelation();
					}else{
						//concatenatedRelations += " ,, " + instance.getOriginalRelation();
						concatenatedRelations += " " + instance.getOriginalRelation();
					}

					relationMatched=true;
				}
			}

		}

		if(!relationMatched){
			ps.println(entity1 + "\t---\t" + entity2 + "\t" + originalSentence);
		}else{
			ps.println(entity1 + "\t"+ concatenatedRelations.trim() +"\t" + entity2 + "\t" + originalSentence);
		}

	}
	
	public void runAndTime(){
		
		System.out.println("Starting Timer");
		final long startTime = System.currentTimeMillis();
		
		this.readSentencesAndRun();
		
		final long endTime = System.currentTimeMillis();
		double secondsElapsed = (endTime-startTime)/1000.0;
		System.out.printf("\nExecution time: %1$.1f s\n", secondsElapsed);
		
	}

	public static void main(String[] rawArgs) throws FileNotFoundException, UnsupportedEncodingException{

		CommandLineParser parser = new BasicParser();

		Options options = new Options();
		options.addOption("p", "parser", true, "Which parser to use. (stanford | malt)");

		CommandLine line = null;

		try {
			line = parser.parse( options, rawArgs );
		}
		catch( ParseException exp ) {
			System.err.println( exp.getMessage() );
			System.exit(1);
		}

		String[] args = line.getArgs();
		String parserName = line.getOptionValue("parser", "stanford");

		System.out.println("Using " + parserName + " parser");

		File evaluationFile = new File(args[0]);
		File outputFile = new File(args[1]);

		BenchmarkBinary evaluation = new BenchmarkBinary(evaluationFile, outputFile, parserName); 

		//read evaluation file with sentences annotated with golden statements
		// and run Exemplar over them.
		evaluation.runAndTime();

	}

}
