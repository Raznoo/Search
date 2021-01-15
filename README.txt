Project partners: Robert Boudreaux
-----------------------------------------------------------------------------------

Instructions for use, describing how a user would interact with your program: 
-----------------------------------------------------------------------------------
To use our program, a user would first input an xml file of interest, as well as titles.txt, docs.txt, and words.txt as inputs into our indexer. Running the indexer would do the parsing of the XML file, tokenizing, stopping/stemming, and pageRank calculations, as well as writing and saving of the txt files with mappings of document IDs to titles in titles.txt, PageRank rankings in docs.txt, and relevance of documents to words in words.txt. 

Afterwards, the user would input a query of interest into the querier, from which the querier will return the titles of documents with the top 10 scores as calculated by term frequency multipled by inverse document frequency (and also potentially factoring in pageRank if specified in the input arguments for indexer). 

A brief overview of your design, including how the pieces of your program fit: 
-----------------------------------------------------------------------------------
Main Sequence of Events:
1)User input (xml filepath, titles.txt filepath, docs.txt filepath, words.txt filepath)
2)Indexer parses information in input xml file
3)Indexer passes parsed information into FileIO
4)FileIO prints relevant information to {titles.txt filepath, docs.txt filepath, words.txt filepath}
5)Query is executed
6)FileIO reads in information from {titles.txt filepath, docs.txt filepath, words.txt filepath} and stores in QueryStencil fields
7)User input for query (string to search for)
8)Search String is tokenized and parsed.
9)Parsed input is compared to QueryStencil Fields to return a ranked list of pages
10)up to first 10 pages are returned

Indexer
    Fields:
        nodeSeq:   //the xml broken up by pages
        numPages:   //the total number of pages in the xml
        regex:   //the given regex that splits on each word/link in a file
        linkReg:  //a subgroup of the given regex that matches only on links
        titlesToID:  //a hashmap of strings representing a page's title mapped to an int representing that same page's ID
        idsToTitles:  //the inverse mapping of titlesToID
        idsToMaxCount:  //a hashmap of integers representing a page's ID mapped to a double representing the frequency of that 
        same page's most frequent word
        idsToPageRanks: //a hashmap of integers representing a page's ID mapped to a double representing the rank of that page
        wordsToDocumentFrequencies: //a hashmap of strings representing a particular stemmed word that appears in the xml
            mapped to a hashmap of integers representing a page's ID that contains (an unstemmed version of) that word and a
            double representing the frequency of appearances of the word
        titlesToLinks:  //a hashmap of strings representing a particular page's title mapped to a hashset of strings representing
            all of the pages that initial title links to within the page's body
    
    parse()
        parse() is the only method in the indexer class. This contains several helper functions that are utilized to
            assist with several pieces of the parsing instruction. Parsing in the context of the indexer class is the process
            of taking in and processing an XML file to a point where the contents of the XML can be divided up into data structures
            compatible with FileIO
        Main Loop: The functionality of parse
            0)loop through each page in nodeSeq
            1)get ID of page
            2)get title of page
            3)store id and title in titlesToID and idsToTitles
            4)isolate the body of the page
            5)convert body of page into a string
            6)tokenize every word in the body of the page by using regex
            7A)if word is a link...
                addLinkToHash method applied to word
            7B)if word is a word(not a link)....
                addtoWordsDocFreq methos applied to word
            8)Execute pagRank

        Helpers:
            addLinkToHash: This is how to update a particular title's list of links that it links to in a page
            addtoWordsDocFreq: this is how to update a word's association to wordsToDocumentFrequencies
            pagRank: This is the method for ranking pages in the XML

    After parse is executed inside of the main method, the indexer class fields are ready to be inputted into FileIO

QueryStencil
    Fields:
        idsToTitles:  //the inverse mapping of titlesToID
        idsToMaxFreq:  //a hashmap of integers representing a page's ID mapped to a double representing the frequency of 
            that same page's most frequent word
        idsToPageRanks: //a hashmap of integers representing a page's ID mapped to a double representing the rank of that page
        wordToInvFreq: //a hashmap of strings representing a particular word mapped to a double representing that word's
            inverse document frequency in the indexed XML
        wordsToDocumentFrequencies: //a hashmap of strings representing a particular stemmed word that appears in the xml
            mapped to a hashmap of integers representing a page's ID that contains (an unstemmed version of) that word and a
            double representing the frequency of appearances of the word
    query
        query is a method in QueryStencil that takes in user input and (after stopping, stemming, and tokenizing input)
            finds the pages in the indexed XML that are most relevant to the page.This contains several helper functions that are 
            utilized to assist with several pieces of the querying instruction.
        Main Loop:
            0)evaluate whether or not to use page rank
            1)tokenize input string
            2)remove tokens that contain stop words
            3)stem remaining tokens
            4)score the pages
            5)if 0 is true then apply pageRankInfluence to page score
            6)turn the scored pages into a list
            7)turn the list into an array containing up to the frist 10 items
            8)print the results to the console
            9) repeat 1-9 until user types :quit

A description of features you failed to implement, as well as any extra features you implemented.
-----------------------------------------------------------------------------------
We implemented all features as specified in the Search handout. We did not implement any extra features :( 

A description of any known bugs in your program.
-----------------------------------------------------------------------------------
We tested out our code on all of the XML files provided, and it ran without issue, so we are not aware of any bugs in our program. 

A description of how you tested your program.
------------------------------------------------------------------------------------
We ran indexer and querier on the BigWiki, MedWiki, SmallWiki, PageRankWiki, and a sample wiki file that mirrored the "less worked-out" example in the handout. Our BigWiki with pagerank took less than five minutes to index and took up less than 300 MB of memory (which is great)! In addition, we tested different queries on the BigWiki, and returned reasonable results (that seemed logical). In addition, if fewer than 10 results were displayed, then our querier returned however many were fetched. When we ran PageRankWiki, Page 100 had a much greater score than the rest of the links (the rest of the links all had the same score), which is consistent with what we would expect from the xml file as well as the project handout. MedWiki took less than 20 seconds to index and again, when we tested the querier, all of the fetched results semeed logical. Lastly, for SmallWiki, it took less than 5 seconds to index, and again, the query results all seemed logical. Thus, we are confident in our code, as we seemed to minimize runtime and had a strong eye for space considerations. 


Written Questions: 
------------------------------------------------------------------------------------
1) With regards to the Breaking News data void, one modification we could make to our design would be to prioritize results in the timeframe between when an event first occurs and when journalists produce content that contain links to already displayed, limited sources (such as Wikipedia, YellowPages, etc. as discussed in the article). Given that harmful and low-quality pages would deliberately be designed to not link to these sources that already contain information, this modification to our design would reduce the spread of inaccurate content. Thus, overall, our search engine design would not only optimize on a particular term, but it would also optimize on how many links a specific page has to other pages that were displayed prior to the outbreak.

With regards to Outdated Terms, one strategy would be to allow search engine users to "flag" outdated articles related to terms they have searched for. An article would be considered outdated if it were over ten years old. Then, having a review board consisting of Congress members as well as top leaders at major tech companies (such as Google, Facebook, etc.) would be in charge of reviewing the flagged pages and drafting standards for what is considered out-of-date. In addition, they would be responsible for reaching out to relevant content creators as well as producing .gov pages with more official information as necessary. This would minimize manipulators' attempts to exploit recency of results. 

2) One vulnerability of our design is that it focuses heavily on term frequency/inverse term frequency with regards to displaying the top ten results. One concern with this is that a page with inaccurate content but a lot of references to the specific query of interest would be ranked highly. Because we don't also consider the rest of the content of the page in relation to the query, this could further the spread of malicious and/or fake news.

In the case where there is little content in the corpus to return, one solution would be to identify synonyms or other closely-related terms to the query, and return results for those new words instead. Thus, we would be slightly modifying the query in the event that little to no matches are found. However, there are ethical concerns to this as well (namely, the fact that users technically wouldn't be receiving results for their exact queries, as their original inputs would be changed). 

3) An organization could ensure their own pages get promoted to the top of search results by having multiple pages link to their own page. This is directly in line with Principle 1 of PageRank. In addition, by strategically manipulating their own pages such that they only link to other pages with few links themselves (Principle 3), and having those pages also belong to the organization, this would again influence their results favorably.
 
If an inaccurate news article has gone viral, this would lead the user of the searh engine to see that page displayed higher in their search results, which would increase their likelihood of viewing it (and hence contribute further to its viral spread). 


