package ca.ualberta.exemplar.core;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import ca.ualberta.relibrarycommon.data.Document;
import ca.ualberta.relibrarycommon.data.EndOfQueue;
import ca.ualberta.relibrarycommon.io.Reader;
import ca.ualberta.relibrarycommon.io.text.ClueWebDocumentReader;
import ca.ualberta.relibrarycommon.io.text.NITFDocumentReader;
import ca.ualberta.relibrarycommon.io.text.PlainTextDocumentReader;
import ca.ualberta.relibrarycommon.io.text.SpinnerDocumentReader;
import ca.ualberta.relibrarycommon.io.text.TACDocumentReader;
import ca.ualberta.relibrarycommon.io.text.WikipediaDocumentReader;

public class Exemplar {

	public static final int QUEUE_SIZE = 100;

	public static void main(String[] args){
		if(args.length != 3){
			System.out.println("Options: input_type (plain|nytimes|wikipedia|tac|clueweb|spinner) input-documents triples");
			System.exit(0);
		}

		Parser parser = new ParserStanford();
		RelationExtraction exemplar = null;
		try {
			exemplar = new RelationExtraction(parser);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		String inputType = args[0];
		File inputDirectory = new File(args[1]);
		File tripleFile = new File(args[2]);

		System.out.println("Starting Exemplar...");

		BlockingQueue<Document> inputQueue = new ArrayBlockingQueue<Document>(QUEUE_SIZE);

		Reader<Document> reader=null;

		if(inputType.startsWith("nytimes")){
			reader = new NITFDocumentReader(inputQueue,inputDirectory);
		}else{
			if(inputType.startsWith("wikipedia")){
				reader = new WikipediaDocumentReader(inputQueue,inputDirectory);
			}else{
				if(inputType.startsWith("plain") || inputType.startsWith("orlando")){
					reader = new PlainTextDocumentReader(inputQueue,inputDirectory);
				}else{
					if(inputType.startsWith("tac")){
						reader = new TACDocumentReader(inputQueue,inputDirectory);
					}else{
						if(inputType.startsWith("clueweb")){
							reader = new ClueWebDocumentReader(inputQueue,inputDirectory);
						}else{
							if(inputType.startsWith("spinner")){
								reader = new SpinnerDocumentReader(inputQueue, inputDirectory);
							}else{
								System.out.println("Invalid input_type: " + inputType);
								System.exit(0);
							}
						}
					}
				}
			}
		}


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
			Document doc=null;
			try {
				doc = inputQueue.take();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			if(doc instanceof EndOfQueue){
				break;
			}

			List<RelationInstance> instances = exemplar.extractRelations(doc.getAsciiText());

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
