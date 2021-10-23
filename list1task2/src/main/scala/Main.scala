import scala.collection.mutable.Map
import scala.io.Source

object Task2 {
  def main(args: Array[String]): Unit = {
    val bookDirectory =
      "../common_sources/lotr.txt"
    val stopWordsDirectory =
      "../common_sources/stop_words_english.txt"

    var bookContent = scala.io.Source.fromFile(bookDirectory,enc="UTF8").mkString
    val partiallyFilteredContent = bookContent.replaceAll(
      """([\p{Punct}&&]|\b\p{IsLetter}{1,2}\b)\s*""",
      " "
    )

    val stopWordsString =
      io.Source
        .fromFile(stopWordsDirectory)
        .getLines
        .mkString("\\b(", "|", ")\\b") //regex with word boundaries
    val fullyFilteredContent =
      ("(?i)" + stopWordsString) //adding case ignore
        .r //compile
        .replaceAllIn(partiallyFilteredContent, "")

    val wordsList = fullyFilteredContent.split("\\s+")
    val mostCommonWords =
      wordsList.groupBy(identity).mapValues(_.size).toSeq.sortBy(-_._2).take(10)
    println(mostCommonWords)

  }
}
