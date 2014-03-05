ABOUT
=====

EXEMPLAR is a relation extraction tool originating from a research project at the
University of Alberta. This work was primarily funded by the NSERC Business 
Intelligence Network (BIN). EXEMPLAR is available free of charge for non-commercial 
use only. All rights are reserved to the respective authors, their institutions 
and members of BIN, who also reserve the right to modify these terms without 
prior notice. EXEMPLAR builds on several tools distributed under different 
licensing models. Please refer to these licenses before using EXEMPLAR.

More information is available at the EXEMPLAR homepage:
https://sites.google.com/a/ualberta.ca/EXEMPLAR/

PEOPLE
======

Filipe Mesquita 
Denilson Barbosa
Jordan Schmidek

INTRODUCTION
============

EXEMPLAR extracts binary and n-ary relationships among entities entities 
(e.g., people, organizations, places) as well as relations among them. 

EXEMPLAR takes text files as input and outputs (subjects, relation, objects)
triples along with a normalized relation and the sentence used for the 
extraction. An example of EXEMPLAR output is:

  SUBJ:Kenworthy#PER <tab> correspondent <tab> POBJ-OF:Washington#LOC,,
  POBJ-FOR:The New York Times <tab> be correspondent <tab> 
  Kenworthy, a Washington correspondent for The New York Times, who ...

where PER and LOC refer to the entity types Person and Location, respectively.
See the Section "ENTITY TYPES" for more entity types. SUBJ, DOBJ and POBJ are
argument roles for subjects, direct object and prepositional objects, 
respectively. POBJ arguments have a suffix determining their preposition 
(e.g. POBJ-FOR for objects of the preposition "for"). Subjects and objects are 
separated by double comma (",,"), if more than one exists.

RUNNING
=======
Note: You will need at least 3GB of free RAM memory to run EXEMPLAR.

Syntax:
$ ./exemplar input-type input-documents triples

Options:
* input-type: the format of the documents. EXEMPLAR accepts the following
format: plain, nytimes, wikipedia, tac, clueweb, spinner. See section
below for more information.

* input-documents: path to the document file or directory containing the 
document files. The tool will recursively look for files in subdirectories.

* triples: path to the file where the triples will be stored. This
is a tab separated file containing one triple per line. The fields are
in order: Subjects, Relation, Objects, Normalized Relation and Sentence. 


FILE FORMATS
============

plain:     Each file represents a document in raw ASCII text.
nytimes:   NITF format, as used by New York Times. http://www.iptc.org/
wikipedia: XML format used in Wikipedia page dump.
tac:	   SGML format used by the Text Analysis Conference (TAC).
clueweb:   WARC format, as used in the ClueWeb09 collection.
spinner:   XML format used by Spinn3r. http://spinn3r.com.

ENTITY TYPES
============

PER:  Person
ORG:  Organization
LOC:  Location
MISC: Miscellaneous

LIBRARIES
=========
The main libraries used in this tool are:

* Stanford Parser:	tokenization, lemmatization, part-of-speech 
		tagging, named entity recognition and dependency parsing.
* Malt Parser:      dependency parsing.
