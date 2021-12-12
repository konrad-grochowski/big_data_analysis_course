def parseBook(bookDirectory:String,k:Int):Set[String] = {

    var bookContent = scala.io.Source.fromFile(bookDirectory,enc="UTF8").mkString
    val partiallyFilteredContent = bookContent.replaceAll(
      """([\p{Punct}&&]|\b\p{IsLetter}{1,2}\b)\s*""",
      " "
    )

    val stopWordsDirectory =
      "../common_sources/stop_words_english.txt"
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
    (0 to wordsList.size-k).map( (x:Int) => wordsList.slice(x,x+k) mkString " ").toSet
}

def jaccard_similiarity(book1:Set[String],book2:Set[String]): Float = {
  book1.intersect(book2).size.toFloat/book1.union(book2).size.toFloat
}


object Main extends App {
  val book1 = parseBook("../common_sources/test1.txt",3)
  val book2 = parseBook("../common_sources/test2.txt",3)
  println(jaccard_similiarity(book1,book2))
}
