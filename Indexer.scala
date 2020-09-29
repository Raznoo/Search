package search.sol

import search.src.{PorterStemmer, StopWords}

import scala.collection.mutable.{HashMap, HashSet}
import scala.util.matching.Regex
import scala.xml.{Node, NodeSeq}

/**
 * a class for handling indexer
 *
 * @param node a node representing a loaded xml file
 */
class Indexer(node: Node) {
  //the xml broken up by pages
  val nodeSeq: NodeSeq = node \ "page"
  //the total number of pages in the xml
  val numPages: Int = nodeSeq.size

  //the given regex that splits on each word/link in a file
  val regex = new Regex("""\[\[[^\[]+?\]\]|[^\W_]+'[^\W_]+|[^\W_]+""")
  //a subgroup of the given regex that matches only on links
  val linkReg = new Regex("""\[\[[^\[]+?\]\]""")

  //a hashmap of strings representing a page's title mapped to an int representing
  //that same page's ID
  val titlesToID: HashMap[String, Int] = new HashMap[String, Int]
  //the inverse mapping of titlesToID
  val idsToTitles: HashMap[Int, String] = new HashMap[Int, String] //done

  //a hashmap of integers representing a page's ID mapped to a double
  //representing the frequency of that same page's most frequent word
  val idsToMaxCount: HashMap[Int, Double] = new HashMap[Int, Double]
  //a hashmap of integers representing a page's ID mapped to a double representing the rank of that page
  var idsToPageRanks: HashMap[Int, Double] = new HashMap[Int, Double]
  //a hashmap of strings representing a particular stemmed word that appears in the xml
  //mapped to a hashmap of integers representing a page's ID that contains (an unstemmed version of) that word and a
  //double representing the frequency of appearances of the word
  val wordsToDocumentFrequencies: HashMap[String, HashMap[Int, Double]] = new HashMap[String, HashMap[Int, Double]]
  //a hashmap of strings representing a particular page's title mapped to a hashset of strings representing
  //all of the pages that initial title links to within the page's body
  val titlesToLinks: HashMap[String, HashSet[String]] = new HashMap[String, HashSet[String]]

  /**
   * a function for parsing an inputted xml file
   */
  def parse() {
    /**
     * updates the pageID's word count of the input word in the wordsToDocFrequencies field
     *
     * @param word   the word to be updated
     * @param pageID the pageID to be updated
     */
    def addtoWordsDocFreq(word: String, pageID: Int) {
      //remove stop words
      if (!StopWords.isStopWord(word)) {
        //stem the word
        val realword = PorterStemmer.stemOneWord(word, new PorterStemmer())
        //add to global word list if it isn't in it already
        wordsToDocumentFrequencies.get(realword) match {
          //if it is currently recorded
          case Some(hashMap) =>
            hashMap.get(pageID) match {
              //if page ID is currently recorded, update frequency & update Idsmax count
              case Some(updateFreq) =>
                wordsToDocumentFrequencies.getOrElse(realword, new HashMap).update(pageID, updateFreq + 1.0)
                idsToMaxCount.get(pageID) match {
                  case Some(existingMaxFreq) =>
                    if (existingMaxFreq < updateFreq + 1) {
                      idsToMaxCount.update(pageID, updateFreq + 1)
                    }
                  //shouldn't get reached
                  case None => idsToMaxCount.put(pageID, updateFreq + 1)

                }
              case None =>
                wordsToDocumentFrequencies.getOrElse(realword, new HashMap).put(pageID, 1.0)
                idsToMaxCount.get(pageID) match {
                  case Some(existingMaxFreq) =>
                    if (wordsToDocumentFrequencies(realword)(pageID) == existingMaxFreq) {
                      idsToMaxCount.update(pageID, existingMaxFreq + 1)
                    }
                  //shouldn't get reached
                  case None => idsToMaxCount.put(pageID, 1)

                }

            }
          //if it needs to be recorded
          case None =>
            val localHash = new HashMap[Int, Double]
            localHash.put(pageID, 1.0)
            wordsToDocumentFrequencies.put(realword, localHash)
            idsToMaxCount.get(pageID) match {
              case Some(existingMaxFreq) =>

                if (existingMaxFreq < 1.0) {
                  idsToMaxCount.update(pageID, 1.0)
                }
              case None => idsToMaxCount.put(pageID, 1.0)
            }
        }
      }
    }

    /**
     * updates a particular title's list of pages that it links to
     *
     * @param link      the link to be added
     * @param pageID    the pageID of the page that needs its link of links updated
     * @param pageTitle the page title of the page that needs its link of links updated
     */
    def addLinkToHash(link: String, pageID: Int, pageTitle: String): Unit = {
      def helper(locLink: String): Unit = {

      }

      val linkTokens = link.split("""\[\[|\||\]\]""").toList
      if (link.contains("|") && linkTokens.size != 2) {
        //multibar case
        if (linkTokens.size - 1 > 2) {
          println("beep")
          //do multibar things
        }
        else if (linkTokens.nonEmpty) {
          println("bop")
          for (word <- regex.findAllIn(linkTokens.tail.tail.head)) {
            addtoWordsDocFreq(word, pageID)
          }
          if (!linkTokens.tail.head.equals(pageTitle)) {
            titlesToLinks.get(pageTitle) match {
              //page is recorded in hashmap and needs to be updated
              case Some(titlesHash) =>
                if (!titlesHash.contains(linkTokens.tail.head)) {
                  titlesToLinks.update(pageTitle, titlesHash.addOne(linkTokens.tail.head))
                }
              //page is not recorded and needs to be added
              case None =>
                val localSet: HashSet[String] = new HashSet[String]
                titlesToLinks.put(pageTitle, localSet.addOne(linkTokens.tail.head))
            }
          }
        }
      } else {
        for (word <- regex.findAllIn(linkTokens.tail.head)) {
          addtoWordsDocFreq(word, pageID)
        }
        if (!linkTokens.tail.head.equals(pageTitle)) {
          titlesToLinks.get(pageTitle) match {
            //page is recorded in hashmap and needs to be updated
            case Some(titlesHash) =>
              if (!titlesHash.contains(linkTokens.tail.head)) {
                titlesToLinks.update(pageTitle, titlesHash.addOne(linkTokens.tail.head))
              }
            //page is not recorded and needs to be added
            case None =>
              val localSet: HashSet[String] = new HashSet[String]
              localSet.addOne(linkTokens.tail.head)
              titlesToLinks.put(pageTitle, localSet)
          }
        }
      }
    }

    /**
     * a method for calculating the page rank of all pages
     */
    def pagRank() = {
      for (title <- titlesToID.keys) {
        titlesToLinks.get(title) match {
          case Some(_) => //do nothing
          case None =>
            val locHash = new HashSet[String]
            for (tinyTitle <- titlesToID.keys) {
              if (!tinyTitle.equals(title)) {
                locHash.addOne(tinyTitle)
              }
            }
            titlesToLinks.put(title, locHash)
        }
      }
      val rprime: Array[Double] = new Array[Double](numPages)
      for (i <- 0 until numPages) {
        rprime(i) = 1.0 / numPages
      }
      //weights being updated
      val r: Array[Double] = new Array[Double](numPages)
      val epsilon = .15
      var currDist: Double = 0.0
      for (i <- 0 until numPages) {
        currDist += ((rprime(i) - r(i)) * (rprime(i) - r(i)))
      }
      currDist = Math.sqrt(currDist)
      while (currDist > .001) {
        for (i <- 0 until numPages) {
          r(i) = rprime(i)
        }
        for (j <- 0 until numPages) {
          rprime(j) = 0
          for (k <- 0 until numPages) {
            if (titlesToLinks(idsToTitles(k)).contains(idsToTitles(j))) {
              rprime(j) += (((epsilon / numPages) +
                ((1.0 - epsilon) * (1.0 / titlesToLinks(idsToTitles(k)).size))) * r(k))
            } else if (idsToTitles(j).equals(idsToTitles(k))) {
              //do nothing because page cannot throw to self
            } else {
              rprime(j) += ((epsilon / numPages) * r(k))
            }
          }
        }
        currDist = 0.0
        for (i <- 0 until numPages) {
          currDist += ((rprime(i) - r(i)) * (rprime(i) - r(i)))
        }
        currDist = Math.sqrt(currDist)
      }
      for (i <- 0 until numPages) {
        println(i)
        idsToPageRanks.put(i, r(i))
      }
    }

    /** where the magic happens :) */
    for (node <- nodeSeq) { //splits on each page
      //getting ID of Page
      val pageTitleBad: String = (node \ "title").text
      val linkFiltReg = new Regex("[\\w].+|[\\w]")
      val pageTitle = linkFiltReg.findAllIn(pageTitleBad).toList.head
      val pageIDStr = (node \ "id").text
      val idFiltReg = new Regex("[0-9]+")
      val pageID = idFiltReg.findAllIn(pageIDStr).toList.head.toInt
      //getting title of page
      idsToTitles.put(pageID, pageTitle)
      titlesToID.put(pageTitle, pageID)
      //making new pageData class associated with pageID and title
      for (word <- regex.findAllIn((node \ "text").text)) { //converts each page body to text
        word match {
          //word is link
          case link if linkReg.matches(link) =>
            addLinkToHash(link, pageID, pageTitle)
          //word is word
          case word =>
            addtoWordsDocFreq(word, pageID)
        }
      }
    }
    //do page ranking
    pagRank()
  }

}


object Indexer {
  def main(args: Array[String]): Unit = {
    val node: Node = xml.XML.loadFile(args(0))
    val mainIndexer: Indexer = new Indexer(node)
    mainIndexer.parse()
    //ARG 1 = titles
    FileIO.printTitleFile(args(1), mainIndexer.idsToTitles)
    //ARG 1 = docs
    FileIO.printDocumentFile(args(2),
      mainIndexer.idsToMaxCount,
      mainIndexer.idsToPageRanks)
    //ARG 2 = words
    FileIO.printWordsFile(args(3), mainIndexer.wordsToDocumentFrequencies)

  }
}