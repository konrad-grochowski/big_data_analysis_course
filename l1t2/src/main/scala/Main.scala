
object Task2 {

  def vMap(graph:Array[(Int,Array[Int])]): Array[(Int,Int)]= {
    graph.flatMap((v1, v2s) => v2s.map( u => (u,v1) ))
  }
  def reduce(graphAcc: Array[(Int,Int)]):Array[(Int, Array[Int])] = {
      graphAcc.groupBy(_._1).mapValues(_.map(_._2)).toArray
  }
  def invertGraph(graph:Array[(Int, Array[Int])]):Array[(Int, Array[Int])] = {
   reduce(vMap(graph))
  }   



  def main(args: Array[String]): Unit = {
     val graph:Array[(Int, Array[Int])] = 
       Array(
        (1, Array(2,3)),
        (3, Array(1, 5)),
        (2, Array(5)),
        (5, Array.empty[Int])
      )

    val invertedGraph = invertGraph(graph)

    for((u,vs) <- invertedGraph){
      println(s"$u: ")
      println(vs.mkString(" "))
    }
  }
}
