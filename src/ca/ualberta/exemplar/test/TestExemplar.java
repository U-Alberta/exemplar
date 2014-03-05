package ca.ualberta.exemplar.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.List;

import ca.ualberta.exemplar.core.RelationExtraction;
import ca.ualberta.exemplar.core.RelationInstance;

public class TestExemplar {
	
public static List<String> readSentences(File file){
		
		BufferedReader reader = null;
		
		try {
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), Charset.forName("UTF-8")));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		List<String> sentenceList = new LinkedList<String>();
		
		String line=null;
		try {
			while(true){
				
				line = reader.readLine();
				if(line == null){
					break;
				}
				
				sentenceList.add(line);
				
			}
		
			reader.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
		
		return sentenceList;
		
	}
	
	public static void main(String[] args) throws FileNotFoundException, UnsupportedEncodingException{
		
		File evaluationFile = new File(args[0]);
		
		RelationExtraction relationExtraction = new RelationExtraction();
		
		List<String> sentences = readSentences(evaluationFile);
		
		for(String sentence: sentences){
			
			try {
				List<RelationInstance> relationInstances = relationExtraction.extractRelations(sentence);
				for(RelationInstance relationInstance : relationInstances){
					System.err.println(relationInstance.detailedString());
				}
			} catch(Exception e){
				e.printStackTrace();
				System.err.println("Resuming...");
			}
					
			
		}
		
		
	}

}
