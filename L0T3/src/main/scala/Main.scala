import scala.collection.mutable.Map
import scala.io._
import scala.io.StdIn.{readLine, readInt}
import java.io.{File, PrintWriter}

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

  var wordsMap: Map[String, Int] =
    scala.collection.mutable.Map().withDefaultValue(0)

  def parseFile(fileDirectory: String): Unit = {
    val fileContent =
      scala.io.Source.fromFile(fileDirectory, enc = "UTF8").mkString
    return parseString(fileContent)
  }
  def parseString(content: String): Unit = {
    val partiallyFilteredContent = content.replaceAll(
      punctuationRegex,
      " "
    )

    val fullyFilteredContent =
      stopWordsRegex.replaceAllIn(partiallyFilteredContent, "")

    val wordsList = fullyFilteredContent.split("\\s+")
    for ((k, v) <- wordsList.groupBy(identity).mapValues(_.size)) {
      wordsMap(k) += v
    }

  }
  def getMostCommonWords(takeNumber: Int): String = {
    wordsMap.toSeq.sortBy(-_._2).take(takeNumber).map((k, v) => s"$v;$k").mkString("\n")
  }
  def createCsvFile(fileName: String, takeNumber: Int): Unit = {
    val csvContent: String = getMostCommonWords(takeNumber)
    println(csvContent)
    val writer = new PrintWriter(new File(fileName))
    writer.write(csvContent)
    writer.close()
  }
}

object Task3 {
  def main(args: Array[String]): Unit = {
    val wordCloudGenerator = WordCloudGenerator()
    while (true) {
      try {
        val parseType = readLine("""
        Type:
          string - to parse string you are going to provide
          file - to parse file whomst've path you are going to provide
          popular - to pring some number most popular words you will provide
          csv - print selected number of words with their weights into csv file
          end - wouldn't you like to know, data boy
        """)
        parseType match {
          case "string" => {
            val stringInput = readLine("Provide the string:")
            wordCloudGenerator.parseString(stringInput)
          }
          case "file" => {
            val fileName = readLine("Provide the fileName:\n")
            wordCloudGenerator.parseFile(fileName)
          }
          case "popular" => {
            println("Provide the Number:")
            val takeNumber: Int = readInt()
            println(wordCloudGenerator.getMostCommonWords(takeNumber))
          }
          case "csv" => {
            val fileName: String = readLine("Provide the filename:\n")
            println("Provide the Number:")
            val takeNumber: Int = readInt()
            wordCloudGenerator.createCsvFile(fileName, takeNumber)
          }
          case "end" => {
            System.exit(0)
          }
          case _ => {
            println("Wrong choice")
          }
        }
      } catch{
        case _:NumberFormatException => println("Think of some better numbers, man")
        case _: Any => println( "Learn to use it first, it doesn't bite")
      }
    }
  }
}
