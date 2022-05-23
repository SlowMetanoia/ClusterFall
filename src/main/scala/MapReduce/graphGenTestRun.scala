package MapReduce

import MapReduce.GraphGenerator.{ ETGQuery, EducationalTrajectory }
import MapReduce.SimpleSplit.ClusterInteractions

import java.io.PrintWriter
import java.lang.Thread.sleep
import java.time.LocalTime
import scala.concurrent.ExecutionContext.global
import scala.concurrent.{ Await, Future }
import scala.concurrent.duration.DurationInt
import scala.util.Random

object graphGenTestRun extends App {
  
  case
  class DiGID(
               sectionsLimits: (Int, Int),
               courseLimits: (Int, Int),
               ioLimits: (Int, Int),
               numberOfQueries: Int
             )
  
  def variateInt( variationFunction: Int => Int )( init: Int ): LazyList[ Int ] =
    init #:: variateInt(variationFunction)(variationFunction(init))
  
  def variateByConst( const: Int ): Int => Int = _ + const
  def variateByDecil:Int=>Int = n=> n + n/10
  
  def variateSections( diGID: DiGID )( variationFunction: Int => Int ): LazyList[ DiGID ] = {
    val (_, courseLimits, ioLimits, numberOfQueries) = DiGID.unapply(diGID).get
    variateInt(variationFunction)(diGID.sectionsLimits._1)
      .zip(variateInt(variationFunction)(diGID.sectionsLimits._2))
      .map(DiGID(_, courseLimits, ioLimits, numberOfQueries))
  }
  
  def variateCourses( diGID: DiGID )( variationFunction: Int => Int ): LazyList[ DiGID ] = {
    val (sectionLimits, _, ioLimits, numberOfQueries) = DiGID.unapply(diGID).get
    variateInt(variationFunction)(diGID.courseLimits._1)
      .zip(variateInt(variationFunction)(diGID.courseLimits._2))
      .map(DiGID(sectionLimits, _, ioLimits, numberOfQueries))
  }
  
  def variateIO( diGID: DiGID )( variationFunction: Int => Int ): LazyList[ DiGID ] = {
    val (sectionLimits, courseLimits, _, numberOfQueries) = DiGID.unapply(diGID).get
    variateInt(variationFunction)(diGID.ioLimits._1)
      .zip(variateInt(variationFunction)(diGID.ioLimits._2))
      .map(DiGID(sectionLimits, courseLimits, _, numberOfQueries))
  }
  
  def variateQueries( diGID: DiGID )( variationFunction: Int => Int ): LazyList[ DiGID ] = {
    val (sectionLimits, courseLimits, ioLimits, _) = DiGID.unapply(diGID).get
    variateInt(variationFunction)(diGID.numberOfQueries)
      .map(DiGID(sectionLimits, courseLimits, ioLimits, _))
  }
  
  def variateAll( diGID: DiGID )( variationFunction: Int => Int ) :LazyList[DiGID] = {
    val (sectionLimits, courseLimits, ioLimits, numberOfQueries) = DiGID.unapply(diGID).get
    val sl = variateInt(variationFunction)(sectionLimits._1) zip
    variateInt(variationFunction)(sectionLimits._1)
    val cl = variateInt(variationFunction)(courseLimits._1) zip
    variateInt(variationFunction)(courseLimits._1)
    val io = variateInt(variationFunction)(ioLimits._1) zip
    variateInt(variationFunction)(ioLimits._1)
    val qn = variateInt(variationFunction)(numberOfQueries)
    sl.zip(cl).zip(io).zip(qn).map{tuple=>
      val (slclio,qn) = tuple
      val (slcl,io) = slclio
      val (sl,cl) = slcl
      DiGID(sl,cl,io,qn)
    }
  }
  var initialDiGID = DiGID(
    sectionsLimits = (2, 8),
    courseLimits = (3, 6),
    ioLimits = (3, 5),
    numberOfQueries = 1
    )
  
  def rand(tuple: (Int, Int)) =
    Random.nextInt(tuple._2 - tuple._1 + tuple._1)
  
  
  def generateCourseTables: DiGID => Seq[ Seq[ Seq[ Int ] ] ] = { digid =>
    val (courseNumLimits, courseInOutLimits, sectionsNumLimits, numOfQueries) = DiGID.unapply(digid).get
    for (_ <- 0 until numOfQueries) yield
      for (_ <- 0 until rand(sectionsNumLimits)) yield
        for (_ <- 0 until rand(courseNumLimits)) yield
          rand(courseInOutLimits)
  }
  
  def generateQueries: DiGID => Seq[ ETGQuery ] =
    generateCourseTables(_).map(table=> ETGQuery(table.length, table.map(_.length), table))
    
  def clusterExec:Seq[ ETGQuery ] => Seq[EducationalTrajectory] = { input=>
    Await.result( GraphGenerator
      .EducationalTrajectoryGeneratorExecutionControl
      .executeInCluster(input), 1.minute)
  }
  def seqExec:Seq[ ETGQuery ] => Seq[EducationalTrajectory] = { input=>
    GraphGenerator
      .EducationalTrajectoryGeneratorExecutionControl
      .executeSequential(input)
  }
  def futureExec:Seq[ ETGQuery ] => Seq[EducationalTrajectory] = { input =>
    Await.result(
      GraphGenerator
        .EducationalTrajectoryGeneratorExecutionControl
        .executeInLocalThreads(input), 1.minute)
  }
  def testAndWright(out:PrintWriter,diGID: DiGID):Unit = {
    val testData = generateQueries(diGID)
    println("started")
    var line = Seq(testData.length,
                   testData.map(_.n).sum/testData.length,
                   testData.flatMap(_.s).sum,
                   testData.head.k.flatten.sum).map(_.toString)
    tick()
    seqExec(testData)
    line = line.appended(tack.toString)
    tick()
    futureExec(testData)
    line = line.appended(tack.toString)
    tick()
    clusterExec(testData)
    line = line.appended(tack.toString)
    out.println(line.mkString(","))
  }
  
  def infiniteVariation(init:DiGID,
                        variationGenerator:DiGID=>(Int=>Int)=>LazyList[DiGID],
                        variationFunction:Int=>Int,
                        filename:String):Unit = {
    val variations = variationGenerator(init)(variationFunction)
    val printer = new PrintWriter(filename)
    printer.println(s"Queries,number of sections,number of courses,number of KASes,Sequential,Local threads,Local cluster")
    variations.foreach(testAndWright(printer,_))
  }
  def infiniteConstVariation =
    infiniteVariation(initialDiGID,_,variateByConst(3),_)
  def infiniteDecilVariation =
    infiniteVariation(initialDiGID,_,variateByDecil,_)
  
  ClusterInteractions.MasterInitialisation()
  //ждём, пока мастер придёт в себя
  sleep(3000)
  
  var curTime: Long = 0
  def tick( ): Unit = curTime = LocalTime.now().toNanoOfDay
  
  def tack = ( LocalTime.now().toNanoOfDay - curTime )
  
  def output: Seq[ EducationalTrajectory ] => Unit = result =>
    println(result
              .map(_.sections
                    .map(_.courses
                          .mkString("\n"))
                    .mkString("\n\n"))
              .mkString(";\n\n\n"))
  
  
  def ScheduleExecution(minutes:Int)(ex:Exception):Unit ={
    Future {sleep(minutes*60000);throw ex}(global)
  }
  
  def terminateAfter(minutes:Int)(code: =>Unit) = {
    try{
      ScheduleExecution(minutes)(new Exception)
      code
    }catch{
      case _=>()
    }
  }
  infiniteConstVariation(
    variateQueries,
    """C:\WorkData\Scala_Projects\ClusterAkk\src\main\resource\everethingQueries.csv"""
    )
  def testAllLocal: Unit ={
    def ivc(vf:DiGID=>(Int=>Int)=>LazyList[DiGID],fileName:String):Unit =
      terminateAfter(30) {
        infiniteConstVariation(vf, """C:\WorkData\Scala_Projects\ClusterAkk\src\main\resource\""" + fileName)
      }
      
    def ivd(vf:DiGID=>(Int=>Int)=>LazyList[DiGID],fileName:String):Unit =
      terminateAfter(30) {
        infiniteDecilVariation(vf, """C:\WorkData\Scala_Projects\ClusterAkk\src\main\resource\""" + fileName)
      }
    
    terminateAfter(500) {
      ivc(variateSections,"variateSectionsC.csv")
      ivc(variateCourses,"variateCoursesC.csv")
      ivc(variateIO,"variateIOC.csv")
      ivc(variateQueries,"variateQueriesC.csv")
  
      ivd(variateSections,"variateSectionsD.csv")
      ivd(variateCourses,"variateCoursesD.csv")
      ivd(variateIO,"variateIOD.csv")
      ivd(variateQueries,"variateQueriesD.csv")
      
      initialDiGID = DiGID(
        sectionsLimits = (30,50),
        courseLimits = (10,20),
        ioLimits = (15,20),
        numberOfQueries = 5
        )
  
      ivc(variateSections,"variateSectionsCLarge.csv")
      ivc(variateCourses,"variateCoursesCLarge.csv")
      ivc(variateIO,"variateIOCLarge.csv")
      ivc(variateQueries,"variateQueriesCLarge.csv")
  
      ivd(variateSections,"variateSectionsDLarge.csv")
      ivd(variateCourses,"variateCoursesDLarge.csv")
      ivd(variateIO,"variateIODLarge.csv")
      ivd(variateQueries,"variateQueriesDLarge.csv")
    }
  }
}
