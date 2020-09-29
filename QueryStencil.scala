package search.sol

import java.io._

import search.src.{PorterStemmer, StopWords}

import scala.collection.mutable.HashMap
import scala.math.log


/**
 * Represents a query REPL built off of a specified index
 *
 * @param titleIndex    - the filename of the title index
 * @param documentIndex - the filename of the document index
 * @param wordIndex     - the filename of the word index
 * @param usePageRank   - true if page rank is to be incorporated into scoring
 */
class Query(titleIndex: String, documentIndex: String, wordIndex: String,
            usePageRank: Boolean) {

  // Maps the document ids to the title for each document
  private val idsToTitle = new HashMap[Int, String]

  // Maps the document ids to the euclidean normalization for each document
  private val idsToMaxFreqs = new HashMap[Int, Double]

  // Maps the document ids to the page rank for each document
  private val idsToPageRank = new HashMap[Int, Double]

  // Maps each word to its inverse document frequency
  private val wordToInvFreq = new HashMap[String, Double]

  // Maps each word to a map of document IDs and frequencies of documents that
  // contain that word
  private val wordsToDocumentFrequencies = new HashMap[String, HashMap[Int, Double]]


  /**
   * Handles a single query and prints out results
   *
   * @param userQuery - the query text
   */
  private def query(userQuery: String) { //big fluffy dog

    //process for stemming and stopping input strings
    val stemmedArray = PorterStemmer.stemArray(userQuery.split("\\s").filter(word => !StopWords.isStopWord(word)))

    /**
     * calculates the score of each page as it corresponds to a particular search query
     *
     * @param strarr the tokenized, stopped and stemmed query represented as an array
     * @return a hashmap of each ID and it's score
     */
    def scorePages(strarr: Array[String]): HashMap[Int, Double] = {
      val scoreMap = new HashMap[Int, Double]

      //calculate idf and tf
      //idf
      //takes in nothing, stores each word's IDF in wordToInvFreq
      def idfCalc(strarr: Array[String]): Unit = {
        for (word <- strarr) {
          wordsToDocumentFrequencies.get(word) match {
            //STD case
            case Some(hashMap) => wordToInvFreq.put(word, log(idsToTitle.size / hashMap.size.toDouble))
            //word is not in page case or word is lkijhdfqgoyb
            case None => //do nothing because word doesn't contribute to query 
          }
        }
      }

      //tf
      // takes in a string, returns a hashmap of id & associated TF
      def tfHashMaker(strarr: Array[String]): HashMap[String, HashMap[Int, Double]] = {
        val tfHash = new HashMap[String, HashMap[Int, Double]]
        for (word <- strarr) {
          wordsToDocumentFrequencies.get(word) match {
            //STD case
            case Some(wordHashMap: HashMap[Int, Double]) =>
              tfHash.put(word, new HashMap[Int, Double])
              for ((id, freq) <- wordHashMap) {
                //gets hashmap associated w/ score in tfHash then updates it to reflect the page's TF
                tfHash.getOrElse(word, null).put(id, freq / idsToMaxFreqs.getOrElse(id, 0.0))
              }
            //word is not in page case or word is lkijhdfqgoyb
            case None => //do nothing because word doesn't contribute to query and we don't want it showing up later
          }
        }
        tfHash
      }
      //does IDF calculations and stores values
      idfCalc(strarr)
      //turn hashmap into series of tuples
      for ((word, hashMap) <- tfHashMaker(strarr)) {
        //turn inner hashmap into series of tuples
        for ((id, termFreq) <- hashMap) {
          scoreMap.get(id) match {
            //ID is already scored and needs to be updated with points added from new word
            case Some(score) => scoreMap.update(id, score + (termFreq * wordToInvFreq.getOrElse(word, 1.0)))
            //ID has never been scored and needs to be added to the hash
            case None => scoreMap.put(id, (termFreq * wordToInvFreq.getOrElse(word, 1.0)))
          }
        }
      }
      scoreMap
    }

    /**
     * influences a hashmap of scores by incorporating a page rank calculation
     *
     * @param scoreHash    the hashmap to be influenced
     * @param pageRankHash the hashmap that does the influencing
     * @return a hashmap of scored that have been influenced by a page rank calculation
     */
    def pageRankInfluence(scoreHash: HashMap[Int, Double], pageRankHash: HashMap[Int, Double]): HashMap[Int, Double] = {
      for ((id, currScore) <- scoreHash) {
        scoreHash.update(id, currScore * pageRankHash.getOrElse(id, 1.0))
      }
      scoreHash
    }

    /**
     * turns a hashmap of page id mapped to scores into a sorted list of page id score tuples
     *
     * @param hash a hashmap of each ID and its score
     * @return a sorted list of page id score tuples
     */
    def hashToSortedList(hash: HashMap[Int, Double]): List[(Int, Double)] = {
      var globList: List[(Int, Double)] = List()

      def addToGlobList(id: Int, score: Double, locList: List[(Int, Double)]): List[(Int, Double)] = {
        locList match {
          case Nil => (id, score) :: List()
          case (storedID, storedScore) :: rest if (score >= storedScore) =>
            (id, score) :: (storedID, storedScore) :: rest
          case (storedID, storedScore) :: rest => (storedID, storedScore) :: addToGlobList(id, score, rest)
        }
      }

      for ((id, value) <- hash) {
        globList = addToGlobList(id, value, globList)
      }
      globList
    }

    /** turns a sorted list into an array of up to 10 integers that represent the best search results
     *
     * @param list      the list to be turned into an array
     * @param currIndex the current index of list
     * @param currArray the current array to be altered
     * @return an array of up to 10 integers that represent the best search results
     */
    def sortedListTo10Array(list: List[(Int, Double)], currIndex: Int, currArray: Array[Int]): Array[Int] = {
      list match {
        case Nil => currArray
        case _ if currIndex >= 10 => currArray
        case (id, _) :: tail =>
          currArray(currIndex) = id
          sortedListTo10Array(tail, currIndex + 1, currArray)
      }
    }

    /** where the magic happens */
    if (usePageRank) {
      val finalList: List[(Int, Double)] =
        hashToSortedList(pageRankInfluence(scorePages(stemmedArray), idsToPageRank))
      printResults(sortedListTo10Array(finalList, 0, new Array[Int](Math.min(10, finalList.size))))
    } else {
      val finalList: List[(Int, Double)] =
        hashToSortedList(scorePages(stemmedArray))
      printResults(sortedListTo10Array(finalList, 0, new Array[Int](Math.min(10, finalList.size))))
    }
  }

  /**
   * Format and print up to 10 results from the results list
   *
   * @param results - an array of all results
   */
  private def printResults(results: Array[Int]) {
    for (i <- 0 until Math.min(10, results.size)) {
      println("\t" + (i + 1) + " " + idsToTitle(results(i)))
    }
  }

  def readFiles(): Unit = {
    FileIO.readTitles(titleIndex, idsToTitle)
    FileIO.readDocuments(documentIndex, idsToMaxFreqs, idsToPageRank)
    FileIO.readWords(wordIndex, wordsToDocumentFrequencies)
  }

  /**
   * Starts the read and print loop for queries
   */
  def run() {
    val inputReader = new BufferedReader(new InputStreamReader(System.in))

    // Print the first query prompt and read the first line of input
    print("search> ")
    var userQuery = inputReader.readLine()

    // Loop until there are no more input lines (EOF is reached)
    while (userQuery != null) {
      // If ":quit" is reached, exit the loop
      if (userQuery == ":quit") {
        inputReader.close()
        return
      }

      // Handle the query for the single line of input
      query(userQuery)

      // Print next query prompt and read next line of input
      print("search> ")
      userQuery = inputReader.readLine()
    }

    inputReader.close()
  }
}

object Query {
  def main(args: Array[String]) {
    try {
      // Run queries with page rank
      var pageRank = false
      var titleIndex = 0
      var docIndex = 1
      var wordIndex = 2
      if (args.size == 4 && args(0) == "--pagerank") {
        pageRank = true;
        titleIndex = 1
        docIndex = 2
        wordIndex = 3
      } else if (args.size != 3) {
        println("Incorrect arguments. Please use [--pagerank] <titleIndex> "
          + "<documentIndex> <wordIndex>")
        System.exit(1)
      }
      val query: Query = new Query(args(titleIndex), args(docIndex), args(wordIndex), pageRank)
      query.readFiles()
      query.run()
    } catch {
      case _: FileNotFoundException =>
        println("One (or more) of the files were not found")
      case _: IOException => println("Error: IO Exception")
    }
  }
}
