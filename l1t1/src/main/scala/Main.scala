import scala.collection.mutable.Map
import scala.io._
import scala.io.StdIn.{readLine, readInt}
import java.io.{File, PrintWriter}
import scala.math._
import math.Numeric.Implicits.infixNumericOps

type Word = String
type DocumentId = String

class WordCloudGenerator() {

  val stopWordsDirectory =
    "../common_sources/stop_words_english.txt"

  val stopWordsString =
    io.Source
      .fromFile(stopWordsDirectory, enc = "UTF-8")
      .getLines
      .mkString("\\b(", "|", ")\\b") //regex with word boundaries

  val stopWordsRegex =
    ("(?i)" + stopWordsString) //adding case ignore
      .r //compile

  val punctuationRegex = """([\p{Punct}&&]|\b\p{IsLetter}{1,2}\b)\s*"""

  var wordsMapOverall: collection.mutable.Map[Word, Int] =
    Map().withDefaultValue(0)

  var wordsMapPerBook: collection.mutable.Map[DocumentId,collection.mutable.Map[Word,Int]] = 
    Map().withDefaultValue(Map().withDefaultValue(0))

  def parseFile(fileDirectory: String,fileName:DocumentId): Unit = {
    val content =
      scala.io.Source.fromFile(fileDirectory, enc = "UTF8").mkString
    val partiallyFilteredContent = content.replaceAll(
      punctuationRegex,
      " "
    )

    val fullyFilteredContent =
      stopWordsRegex.replaceAllIn(partiallyFilteredContent, "")

    val wordsList = fullyFilteredContent.split("\\s+")
    wordsMapPerBook(fileName) = wordsList.groupBy(identity).mapValues(_.size).to(collection.mutable.Map)
    for ((k, v) <- wordsMapPerBook(fileName)) {
      wordsMapOverall(k) += v
    }

  }

  def getMostCommonWords[T](takeNumber: Int,wordsMap:collection.mutable.Map[Word,T])(implicit num: Numeric[T]): String = {
    wordsMap.toSeq.sortBy(-_._2).take(takeNumber).map((k, v) => s"$k - $v").mkString("\n")
  }

  def getMostCommonWordsOverall(takeNumber: Int): String = {
    getMostCommonWords(takeNumber,wordsMapOverall)
  }

  def getMostCommonWordsPerBook(takeNumber: Int,fileName:DocumentId): String = {
    getMostCommonWords(takeNumber,wordsMapPerBook(fileName))
  }

  def calculateTfIdf():Map[DocumentId,Map[Word,Double]] = {
    def filesWithTheWord(word:Word):Int = {
        wordsMapPerBook.filter((fileName,map) => map.getOrElse(word,0) > 0).size
    }
    var tfIdfMap: Map[DocumentId,Map[Word,Double]] = Map()
    val filesNumber:Double = wordsMapPerBook.keys.size
    for ((documentId, wordsMap) <- wordsMapPerBook){
      val wordsNumber = wordsMap.values.sum
      tfIdfMap(documentId) = 
        wordsMap.map(
          (word,number) => 
            (word, number.toDouble/wordsNumber * log10(filesNumber.toDouble/filesWithTheWord(word)))
        ).to(collection.mutable.Map)
    }
    tfIdfMap

  }

}

object Task1 {
  def main(args: Array[String]): Unit = {
    val wordCloudGenerator = WordCloudGenerator()
    wordCloudGenerator.parseFile("../common_sources/lotr.txt","cm")
    wordCloudGenerator.parseFile("../common_sources/manifesto.txt","lotr")
    val tfIdf = wordCloudGenerator.calculateTfIdf()
    val lotrTfIdf = tfIdf("lotr")
    val cmTfIdf = tfIdf("cm")

    println("Most popular overall:")
    println(wordCloudGenerator.getMostCommonWordsOverall(10))
    println("\n\nMost popular in book 1:")
    println(wordCloudGenerator.getMostCommonWordsPerBook(10,"lotr"))
    println("\nMost popular in book 2:")
    println(wordCloudGenerator.getMostCommonWordsPerBook(10,"cm"))
    println("\nTF.IDF in book 1:")
    println(wordCloudGenerator.getMostCommonWords(10,lotrTfIdf))
    println("\nTF.IDF in book 2:")
    println(wordCloudGenerator.getMostCommonWords(10,cmTfIdf))
    println("")
  }
}
