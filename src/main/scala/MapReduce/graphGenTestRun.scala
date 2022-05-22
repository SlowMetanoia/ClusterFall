package MapReduce

import MapReduce.GraphGenerator.ETGQuery
import MapReduce.SimpleSplit.ClusterInteractions

import scala.concurrent.ExecutionContext.global
import scala.util.Random

object graphGenTestRun extends App{
  val courseNumLimits = (3,6)
  val courseInOutLimits = (3,7)
  val sectionsNumLimits = (8,8)
  def rand(tpl:(Int,Int)) = Random.nextInt(tpl._2-tpl._1+1)+tpl._1
  def randomCourseTable:Seq[Seq[Int]] =
    for(_<-0 until rand(sectionsNumLimits)) yield
      for(_<-0 until rand(courseNumLimits)) yield
        rand(courseInOutLimits)
  
  def testDataGen:LazyList[ETGQuery] = {
    val table = randomCourseTable
    ETGQuery(table.length,table.map(_.length),table)#::testDataGen
  }
  val tdg = testDataGen
  def makeReadable(etgQuery: ETGQuery): String ={
    s"Query(\n" +
      s"${etgQuery.n}\n"+
      s"${etgQuery.s}\n"+
      s"table:\n"+
      s"${etgQuery.k.mkString("\n")}\n"
  }
  ClusterInteractions.MasterInitialisation()
  
  println(makeReadable(tdg.take(1).head))
  /*println(GraphGenerator.EducationalTrajectoryGeneratorExecutionControl.executeSequential(
    tdg.take(1).force
    ).map(_.sections.map(_.courses.mkString("\n")).mkString("\n\n")).mkString(";\n\n\n"))
  */
  GraphGenerator.EducationalTrajectoryGeneratorExecutionControl.executeInCluster(
    tdg.take(1).force).onComplete(_.map(result =>
                                   println(result
                                             .map(_.sections
                                                   .map(_.courses
                                                         .mkString("\n"))
                                                   .mkString("\n\n"))
                                             .mkString(";\n\n\n")))
  )(global)
}
