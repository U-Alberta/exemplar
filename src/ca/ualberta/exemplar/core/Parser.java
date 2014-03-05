package ca.ualberta.exemplar.core;

import java.util.List;

import edu.stanford.nlp.util.CoreMap;

public interface Parser {
	
	public List<CoreMap> parseText(String text);

}
