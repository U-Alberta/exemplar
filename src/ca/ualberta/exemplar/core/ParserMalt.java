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
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.maltparser.MaltParserService;
import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.symbol.SymbolTable;
import org.maltparser.core.syntaxgraph.DependencyGraph;
import org.maltparser.core.syntaxgraph.edge.Edge;
import org.maltparser.core.syntaxgraph.node.DependencyNode;

import ca.ualberta.exemplar.util.Paths;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.trees.EnglishGrammaticalStructure;
import edu.stanford.nlp.trees.GrammaticalRelation;
import edu.stanford.nlp.trees.TreeGraphNode;
import edu.stanford.nlp.trees.TypedDependency;
import edu.stanford.nlp.trees.semgraph.SemanticGraph;
import edu.stanford.nlp.trees.semgraph.SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation;
import edu.stanford.nlp.util.CoreMap;

public class ParserMalt implements Parser {

    private MaltParserService maltParser;
    
    private Map<String,String> relationMap;

    private StanfordCoreNLP pipeline;
	
	public ParserMalt(){
		
		try {

			Properties props = new Properties();
			props.put("customAnnotatorClass.cleanprefix", "ca.ualberta.exemplar.core.CleanPrefixAnnotator");
			props.put("customAnnotatorClass.locationjuxtaposition", "ca.ualberta.exemplar.core.LocationJuxtapositionAnnotator");
			props.put("customAnnotatorClass.removedashes", "ca.ualberta.exemplar.core.RemoveDashesAnnotator");
			props.put("annotators", "tokenize, ssplit, removedashes, pos, lemma, ner, cleanprefix, locationjuxtaposition");
			pipeline = new StanfordCoreNLP(props);

			maltParser = new MaltParserService();
			maltParser.initializeParserModel("-c " + Paths.MALT_PARSER_FILENAME
					+ " -w " + Paths.MALT_PARSER_DIRECTORY + " -m parse");
			
			// Used to map from malt relations to Stanford relations
			relationMap = new HashMap<String,String>();
			relationMap.put("measure", "npadvmod");

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private String[] sentenceToCoNLLInput(List<CoreLabel> tokens){
		List<String> conllList = new ArrayList<String>(100);
		
		int num = 1;
		for (CoreLabel token : tokens) {
			
			String word = token.word();
			String lemmaA = token.lemma();
			String lemma  = lemmaA != null && lemmaA.length() > 0 ? lemmaA : "_";
			String posA = token.get(PartOfSpeechAnnotation.class);
			String pos  = posA != null && posA.length() > 0 ? posA : "_";
			
			conllList.add(num+"\t"+word+"\t"+lemma+"\t"+pos+"\t"+pos+"\t"+"_");
			
			num++;
		}
		
		String[] conll = new String[conllList.size()];
		conll = conllList.toArray(conll);
		return conll;
	}
	
	class Result {
		public List<List<String>> conll;
		public int rootIndex;
	}
	private Result graphToCoNLL(DependencyGraph graph) throws MaltChainedException{
		List<List<String>> conll = new ArrayList<List<String>>();
		int rootIndex = 0;
		for (int i = 1; i <= graph.getHighestDependencyNodeIndex(); i++) {
			DependencyNode node = graph.getDependencyNode(i);
			List<String> line = new ArrayList<String>();
			if (node != null) {
				for (SymbolTable table : node.getLabelTypes()) {
					line.add(node.getLabelSymbol(table));
				}
				if (node.hasHead()) {
					Edge  e = node.getHeadEdge();
					int index = e.getSource().getIndex();
					if(index == 0){
						rootIndex = node.getIndex();
					}
					line.add(String.valueOf(index));
					if (e.isLabeled()) {
						for (SymbolTable table : e.getLabelTypes()) {
							String relation = e.getLabelSymbol(table);
							if(relationMap.containsKey(relation))
								relation = relationMap.get(relation);
							line.add(relation);
						}
					} else {
						for (SymbolTable table : graph.getDefaultRootEdgeLabels().keySet()) {
							line.add(graph.getDefaultRootEdgeLabelSymbol(table));
						}
					}
				}
				conll.add(line);
				line = new ArrayList<String>();
			}
		}
		Result result = new Result();
		result.conll = conll;
		result.rootIndex = rootIndex;
		return result;
	}

	@Override
	public List<CoreMap> parseText(String text) {
		
		List<CoreMap> sentences = null;
		
		try {
			Annotation document = new Annotation(text);
			pipeline.annotate(document);

			sentences = document.get(SentencesAnnotation.class);
			for(CoreMap sentence : sentences){
				
				List<CoreLabel> tokens = sentence.get(TokensAnnotation.class);
				String[] conllInput = sentenceToCoNLLInput(tokens);
				
				DependencyGraph graph = (DependencyGraph) maltParser.parse(conllInput);
				
				Result result = graphToCoNLL(graph);
				List<List<String>> conll = result.conll;
				int rootIndex = result.rootIndex;

				EnglishGrammaticalStructure gs = EnglishGrammaticalStructure.buildCoNNLXGrammaticStructure(conll);
				
				
				TreeGraphNode root = null;

				List<TypedDependency> deps = gs.typedDependenciesCCprocessed();
				
				// Add root dependency and ner annotations
				int size = deps.size();
				for(int i = 0; i < size; i++){
					TypedDependency td = deps.get(i);
					if(td.gov().index() == rootIndex){
						root = td.gov();
						deps.add(new TypedDependency(GrammaticalRelation.ROOT, td.gov(), td.gov()));
					}
					{
						TreeGraphNode n = td.dep();
						if(n.label().ner() == null){
							n.label().setNER(tokens.get(n.index()-1).ner());
							n.label().setBeginPosition(tokens.get(n.index()-1).beginPosition());
							n.label().setEndPosition(tokens.get(n.index()-1).endPosition());
							n.label().setLemma(tokens.get(n.index()-1).lemma());
						}
					}
					{
						TreeGraphNode n = td.gov();
						if(n.label().ner() == null){
							n.label().setNER(tokens.get(n.index()-1).ner());
							n.label().setBeginPosition(tokens.get(n.index()-1).beginPosition());
							n.label().setEndPosition(tokens.get(n.index()-1).endPosition());
							n.label().setLemma(tokens.get(n.index()-1).lemma());
						}
					}
				}
				if(root == null)
					continue;
				
				List<TreeGraphNode> roots = new ArrayList<TreeGraphNode>();
				roots.add(gs.root());

				SemanticGraph sg = new SemanticGraph(deps, roots);
				sentence.set(CollapsedCCProcessedDependenciesAnnotation.class, sg);

			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return sentences;
	}
	
	public static void main(String[] args) throws IOException, MaltChainedException{
		File textFile = new File(args[0]);
		
		
		BufferedReader br = new BufferedReader(new FileReader(textFile));
		String line = br.readLine();
		ParserMalt parser = new ParserMalt();
		parser.parseText(line);
		br.close();
	}

}
