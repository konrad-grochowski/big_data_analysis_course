
object Task1 {

  def foldFunction(map: scala.collection.mutable.Map[Int, Array[Int]], values:Array[Int]):scala.collection.mutable.Map[Int, Array[Int]] = {
    map(values(0)) = Array(map(values(0))(0),map(values(0))(1)+1)
    map(values(1)) = Array(map(values(1))(0)+1,map(values(1))(1))
    map
  }


  def main(args: Array[String]): Unit = {
  val graphMap = scala.io.Source.fromFile("file.txt")
    .getLines
    .map(_.split("	").map(_.toInt))
    .foldLeft(scala.collection.mutable.Map[Int, Array[Int]]().withDefaultValue(Array.fill[Int](2)(0)))(foldFunction)  
    graphMap foreach {case (key, value) => println (key + "-->" + (value mkString ", "))}
  }
  
}
