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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import ca.ualberta.exemplar.evaluation.BenchmarkBinary;
import ca.ualberta.exemplar.evaluation.BenchmarkNary;

public class Exemplar {

	public static final int QUEUE_SIZE = 100;

	public static void main(String[] rawArgs) throws FileNotFoundException, UnsupportedEncodingException{
		
		CommandLineParser cli = new BasicParser();
		
		Options options = new Options();
		options.addOption("h", "help", false, "shows this message");
		options.addOption("b", "benchmark", true, "expects input to be a benchmark file (type = binary | nary)");
		options.addOption("p", "parser", true, "defines which parser to use (parser = stanford | malt)");
		
		CommandLine line = null;
		
		try {
	        line = cli.parse( options, rawArgs );
	    }
	    catch( ParseException exp ) {
	        System.err.println( exp.getMessage() );
	        System.exit(1);
	    }
		
		String[] args = line.getArgs();
		String parserName = line.getOptionValue("parser", "malt");
		
				
		if(line.hasOption("help")){
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp( "sh ./exemplar", options );
			System.exit(0);
		}
		
		if(args.length != 2){
			System.out.println("error: exemplar requires an input file and output file.");
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp( "sh ./exemplar <input> <output>", options );
			System.exit(0);
		}

		File input = new File(args[0]);
		File output = new File(args[1]);
		
		String benchmarkType = line.getOptionValue("benchmark", "");
		if(!benchmarkType.isEmpty()){
			if(benchmarkType.equals("binary")){
				BenchmarkBinary evaluation = new BenchmarkBinary(input, output, parserName); 
				evaluation.runAndTime();
				System.exit(0);
			}
			else{
				if(benchmarkType.equals("nary")){
					BenchmarkNary evaluation = new BenchmarkNary(input, output, parserName); 
					evaluation.runAndTime();
					System.exit(0);
				}else{
					System.out.println("error: benchmark option has to be either 'binary' or 'nary'.");
					System.exit(0);
				}
			}
		}

		Parser parser = null;
		if (parserName.equals("stanford")){
			parser = new ParserStanford();
		}else{
			if (parserName.equals("malt")){
				parser = new ParserMalt();
			}else{
				System.out.println(parserName + " is not a valid parser.");
				System.exit(0);
			}
		}

		System.out.println("Starting EXEMPLAR...");
		
		RelationExtraction exemplar = null;
		try {
			exemplar = new RelationExtraction(parser);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		BlockingQueue<String> inputQueue = new ArrayBlockingQueue<String>(QUEUE_SIZE);
		PlainTextReader reader=null;
		reader = new PlainTextReader(inputQueue,input);

		Thread readerThread = new Thread(reader);
		readerThread.start();

		PrintStream statementsOut = null;

		try {
			statementsOut = new PrintStream(output, "UTF-8");
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
			System.exit(0);
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
			System.exit(0);
		}

		statementsOut.println("Subjects\tRelation\tObjects\tNormalized Relation\tSentence");

		while(true){
			String doc=null;
			try {
				doc = inputQueue.take();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			if(doc.isEmpty()){
				break;
			}

			List<RelationInstance> instances = exemplar.extractRelations(doc);

			for(RelationInstance instance : instances){


				// Output SUBJ arguments in a separate field, for clarity
				boolean first = true;
				for (Argument arg : instance.getArguments()){
					if(arg.argumentType.equals("SUBJ")){
						if(first){
							first = false;
						}else{
							statementsOut.print(",,");
						}
						statementsOut.print(arg.argumentType + ":" + arg.entityId);
					}
				}

				// Output the original relation
				statementsOut.print("\t" + instance.getOriginalRelation() + "\t");

				// Output the DOBJ arguments, followed by POBJ
				first = true;
				for (Argument arg : instance.getArguments()){
					if(arg.argumentType.equals("DOBJ")){
						if(first){
							first = false;
						}else{
							statementsOut.print(",,");
						}
						statementsOut.print(arg.argumentType + ":" + arg.entityId);
					}
				}
				for (Argument arg : instance.getArguments()){
					if(arg.argumentType.startsWith("POBJ")){
						if(first){
							first = false;
						}else{
							statementsOut.print(",,");
						}
						statementsOut.print(arg.argumentType + ":" + arg.entityId);
					}
				}
				statementsOut.print("\t" + instance.getNormalizedRelation());
				statementsOut.print("\t" + instance.getSentence());
				statementsOut.println();
			}
		}

		System.out.println("Done!");
		statementsOut.close();



	}

}
