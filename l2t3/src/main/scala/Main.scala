import scala.util.Random

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

def generateMinHashes(number:Int,size:Int): Array[Array[Int]] = {
  (0 until number).map((x) => Random.shuffle(0 until size).toArray).toArray
}

def generateSignature(books: Array[Set[String]], minHashes: Array[Array[Int]],allShinglesArray:Array[String]): Array[Array[Int]] = {

  books.map(
    (book) =>
    minHashes.map(
      (minHash) =>
          minHash.find((perm) => book.contains(allShinglesArray(perm))).get
        
      
    )
  )

}
object Main extends App {
  val book1 = parseBook("../common_sources/lotr.txt",3)
  val book2 = parseBook("../common_sources/lotr.txt",3)
  val books = Array(book1,book2)

  var allShinglesSet:Set[String] = Set()
  for (book <- books){
    allShinglesSet ++= book
  }
  // println(allShinglesSet)
  val minHashes = generateMinHashes(10,allShinglesSet.size);
  val allShinglesArray:Array[String] = allShinglesSet.toArray.sorted
  val bookSignatures = generateSignature(Array(book1,book2),minHashes,allShinglesArray)
  // allShinglesArray.map(println)
  // bookSignatures.map((x) => x.map(println))
  println(jaccard_similiarity(book1,book2))
  println(bookSignatures(0).zip(bookSignatures(1)).map((a,b) => a==b).count(_ == true))
}
