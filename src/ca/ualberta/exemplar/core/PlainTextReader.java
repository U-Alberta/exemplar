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
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import ca.ualberta.exemplar.util.StringUtils;


/**
 * PlainTextReader 
 * 
 * recursively parses the input directory specified for any .txt files present.
 * 
 * @author Filipe Mesquita <mesquita@ualberta.ca>
 */

public class PlainTextReader implements Runnable {

	protected BlockingQueue<String> queue;
	protected File inputDirectory;
	protected String fileExtension;
	protected int numFiles;

	public PlainTextReader(BlockingQueue<String> loadDocumentQueue, File directory) {
		this.queue = loadDocumentQueue;
		this.inputDirectory = directory;
		this.fileExtension = ".txt";
	}

	/**
	 * This implementation parses a text file and generates Documents. 
	 * 
	 * @param inputFile the input file to be parsed
	 */
	public void readFile(File inputFile) {

		BufferedReader reader = null;

		try {

			//System.out.println(">> Opening file: " + inputFile.toString() + " for parsing");
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(inputFile), Charset.forName("UTF-8")));

			StringBuffer fileText = new StringBuffer(1000);

			while(true){
				String line = reader.readLine();
				//EOF
				if(line == null)
					break;
				fileText.append(line + "\n");
			}
			reader.close();

			String text = fileText.toString();
			text = StringUtils.convertToAscii(text);
			// Empty document signal end of queue. Avoid stopping early by sending a white space instead.
			if(text.isEmpty())
				text = " ";
			loadDocument(text);

		} catch (Exception e){
			System.err.println("Failed to open input file [" +inputFile.getAbsoluteFile()+ "] in loader");
			e.printStackTrace();
			return;
		}

	} 


	private void loadDocument(String document) {

		try {
			queue.put(document);
			numFiles++;

			if(numFiles % 1000 == 0){
				System.out.println("[" + now() + "] Documents read so far: " + numFiles);
			}


		} catch (Exception e){
			e.printStackTrace();
		}

	}

	public static void main(String args[]){

		String inputFileName = args[0];

		BlockingQueue<String> queue = new ArrayBlockingQueue<String>(100);
		File inputFile = new File(inputFileName);
		PlainTextReader reader = new PlainTextReader(queue, inputFile);

		Thread thread = new Thread(reader);
		thread.start();

		while(true){
			String document=null;

			try {
				document = queue.take();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			if(document.isEmpty()){
				break;
			}
		}

		try {
			thread.join();
		} catch(Exception e){
			e.printStackTrace();
		}

	}

	public static String now() {
		Calendar cal = Calendar.getInstance();
		String format = "yyyy-MM-dd HH:mm:ss";
		SimpleDateFormat sdf = new SimpleDateFormat(format);
		return sdf.format(cal.getTime());

	}

	/**
	 * This method recursively parses <code>directory</code> looking for files 
	 * whose names end with <code>extension</code>. 
	 *  
	 * @param directory the directory where input files are located.
	 */
	public void readDirectory(File currentDir){

		if(currentDir == null || currentDir.isHidden()){
			return;
		}

		if(fileExtension == null){
			fileExtension = "";
		}

		if (currentDir.isFile() && (currentDir.getName().endsWith(fileExtension))) {
			readFile(currentDir);
		}

		if (currentDir.isDirectory()) {
			File[] files = currentDir.listFiles();
			for (int i = 0; i < files.length; i++) {
				readDirectory(files[i]);
			}
		}
	}


	@Override
	public void run() {
		readDirectory(inputDirectory);

		System.out.println(">> Finished reading all input files...");
		System.out.println(">> Number of files: " + numFiles);

		// Put a poison object signaling that all files have been read
		try {
			queue.put(new String());
		} catch (InterruptedException ie) {
			ie.printStackTrace();
		}

	}

}


