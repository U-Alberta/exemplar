package ca.ualberta.exemplar.core;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class Exemplar {

	public static final int QUEUE_SIZE = 100;

	public static void main(String[] args){
		if(args.length != 3){
			System.out.println("Options: parser(stanford|malt) input-documents triples");
			System.exit(0);
		}

		String parserName = args[0];
		File inputDirectory = new File(args[1]);
		File tripleFile = new File(args[2]);

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

		System.out.println("Starting Exemplar...");
		
		RelationExtraction exemplar = null;
		try {
			exemplar = new RelationExtraction(parser);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		BlockingQueue<String> inputQueue = new ArrayBlockingQueue<String>(QUEUE_SIZE);
		PlainTextReader reader=null;
		reader = new PlainTextReader(inputQueue,inputDirectory);

		Thread readerThread = new Thread(reader);
		readerThread.start();

		PrintStream statementsOut = null;

		try {
			statementsOut = new PrintStream(tripleFile, "UTF-8");
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
