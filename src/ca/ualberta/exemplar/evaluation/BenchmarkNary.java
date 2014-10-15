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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import ca.ualberta.exemplar.core.Argument;
import ca.ualberta.exemplar.core.RelationExtraction;
import ca.ualberta.exemplar.core.Parser;
import ca.ualberta.exemplar.core.ParserMalt;
import ca.ualberta.exemplar.core.ParserStanford;
import ca.ualberta.exemplar.core.RelationInstance;
import edu.stanford.nlp.util.StringUtils;

public class BenchmarkNary  {

	private File evaluationFile;
	PrintStream ps;
	RelationExtraction relationExtraction;
	
	public static final String PLACEHOLDER_ENTITY = "Europe";
	private Map<Integer,String> realEntities = new HashMap<Integer,String>();
	
	public BenchmarkNary(File evaluationFile, File outputFile) throws FileNotFoundException, UnsupportedEncodingException{
		this(evaluationFile, outputFile, null);
	}
	
	public BenchmarkNary(File evaluationFile, File outputFile, String parserName) throws FileNotFoundException, UnsupportedEncodingException{
		this.evaluationFile = evaluationFile;
		
		Parser parser;
		if("malt".equalsIgnoreCase(parserName)){
			parser = new ParserMalt();
		}else{
			parser = new ParserStanford();
		}

		relationExtraction = new RelationExtraction(parser);

		ps = new PrintStream(outputFile, "UTF-8");
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

		
		int correctArguments = 0;
		int systemArguments = 0;
		int gtArguments = 0;
		
		String line=null;
		int count=0;
		try {
			while(true){

				line = reader.readLine();
				count++;

				if(line == null){
					break;
				}

				if(count % 1000 == 0){
					System.out.println("Sentences processed in the NLP pipeline (so far): " + count);
				}
				
				String[] tokens = line.split(" ");

				StringBuilder sentence = new StringBuilder();
				
				int entityCount=0;
				boolean entityOpen=false;
				String trigger=null;
				int newTokenIndex = 0;
				String entityName = "";
				
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
						trigger = tokens[i]; 
					}
					
					if(entityOpen){
						entityName += tokens[i] + " ";
					}

					if(tokens[i].indexOf("[[[") == 0){
						entityOpen=true;
					}

					if(entityOpen == false){
						sentence.append(tokens[i] + " ");
						newTokenIndex++;
					}
					
					if(tokens[i].indexOf("]]]") > 0){
						tokens[i] = tokens[i].replaceAll("\\]", "");
						realEntities.put(newTokenIndex, entityName.replaceAll("\\]", "").trim());
						entityName = "";
						entityOpen = false;
						entityCount++;
						sentence.append(PLACEHOLDER_ENTITY + " ");
						newTokenIndex++;
					}

				}

				// Workaround to keep tokens as in original sentence.
				String newtext = sentence.toString().trim();
				newtext = newtext.replaceAll("-LRB-", "(");
				newtext = newtext.replaceAll("-RRB-", ")");
				newtext = newtext.replaceAll("\\\\/", "/");
				newtext = newtext.replaceAll("\\. ,", "\\.,");

				gtArguments += entityCount;
				
				List<RelationInstance> instances = run(newtext);
				
				boolean foundRelation = false;
				for (RelationInstance instance : instances){
					
					if(trigger == null){
						System.out.println("Trigger null!");
						break;
					}
					
					if(instance.getOriginalRelation().indexOf(trigger) >= 0){
						foundRelation = true;
						
						ps.print(trigger + "\t");
						List<Argument> args = instance.getArguments();
						if(args.size() > 0){
							Vector<String> names = new Vector<String>(args.size());
							for(Argument arg : args){
								names.add(realEntities.get(arg.getStartIndex()));
							}
							ps.print(StringUtils.join(names, ",,"));
						}else{
							ps.print("---");
						}
						ps.print("\t");
						
						for(Argument arg : args){
							systemArguments++;
							if(arg.getEntityName().equals(PLACEHOLDER_ENTITY)){
								correctArguments++;
							}
						}
						break;
					
					}
				}
				if(!foundRelation){
					ps.print("---\t---\t");
				}
				ps.println(line);
			}

			reader.close();
			
			System.out.println("Precision: " + (double) correctArguments/ (double) systemArguments);
			System.out.println("Recall: " + (double) correctArguments/ (double)gtArguments);
			System.out.println("Correct : " + correctArguments);
			System.out.println("System : " + systemArguments);
			System.out.println("Total : " + gtArguments);

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private List<RelationInstance> run(String normalizedSentence) throws FileNotFoundException, UnsupportedEncodingException {
		List<RelationInstance> instances = null;

		try {
			instances = relationExtraction.extractRelations(normalizedSentence);

		} catch(Exception e){
			e.printStackTrace();
			System.out.println("Resuming...");
		}

		if(instances!=null && instances.size()>0){

			for(RelationInstance instance : instances){

				System.out.println("Instance: " + instance.getOriginalRelation());
				
				for(Argument arg : instance.getArguments()){
					System.out.println("\tArg: [" + arg.getEntityName() +"] - Type: " + arg.getArgumentType());
				}

			}
		}
		return instances;
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

		BenchmarkNary evaluation = new BenchmarkNary(evaluationFile, outputFile, parserName); 

		//read evaluation file with sentences annotated with golden statements
		// and run Exemplar over them.
		evaluation.runAndTime();

	}

}
