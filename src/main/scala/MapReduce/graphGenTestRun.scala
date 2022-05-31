package MapReduce

import MapReduce.GraphGenerator.{ ETGQuery, EducationalTrajectory, GraphManager }
import MapReduce.SimpleSplit.ClusterInteractions

import java.io.PrintWriter
import java.lang.Thread.sleep
import java.time.LocalTime
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.concurrent.{ Await, Future }
import scala.util.{ Random, Try }

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
  
  def variateByDecil: Int => Int = n => n + n / 10 + 1
  
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
  
  def variateAll( diGID: DiGID )( variationFunction: Int => Int ): LazyList[ DiGID ] = {
    val (sectionLimits, courseLimits, ioLimits, numberOfQueries) = DiGID.unapply(diGID).get
    val sl = variateInt(variationFunction)(sectionLimits._1) zip
      variateInt(variationFunction)(sectionLimits._1)
    val cl = variateInt(variationFunction)(courseLimits._1) zip
      variateInt(variationFunction)(courseLimits._1)
    val io = variateInt(variationFunction)(ioLimits._1) zip
      variateInt(variationFunction)(ioLimits._1)
    val qn = variateInt(variationFunction)(numberOfQueries)
    sl.zip(cl).zip(io).zip(qn).map { tuple =>
      val (slclio, qn) = tuple
      val (slcl, io) = slclio
      val (sl, cl) = slcl
      DiGID(sl, cl, io, qn)
    }
  }
  
  var initialDiGID = DiGID(
    sectionsLimits = (2, 8),
    courseLimits = (3, 6),
    ioLimits = (3, 5),
    numberOfQueries = 1
    )
  
  def rand( tuple: (Int, Int) ) =
    Random.nextInt(tuple._2 - tuple._1) + tuple._1 + 1
  
  
  def generateCourseTables: DiGID => Seq[ Seq[ Seq[ Int ] ] ] = { digid =>
    val (courseNumLimits, courseInOutLimits, sectionsNumLimits, numOfQueries) = DiGID.unapply(digid).get
    for (_ <- 0 until numOfQueries) yield
      for (_ <- 0 until rand(sectionsNumLimits)) yield
        for (_ <- 0 until rand(courseNumLimits)) yield
          rand(courseInOutLimits)
  }
  
  def generateQueries: DiGID => Seq[ ETGQuery ] =
    generateCourseTables(_).map(table => ETGQuery(table.length, table.map(_.length), table))
  
  def clusterExec: Seq[ ETGQuery ] => Seq[ EducationalTrajectory ] = { input =>
    Await.result(GraphGenerator
                   .EducationalTrajectoryGeneratorExecutionControl
                   .executeInCluster(input), 10.minute)
  }
  
  def seqExec: Seq[ ETGQuery ] => Seq[ EducationalTrajectory ] = { input =>
    GraphGenerator
      .EducationalTrajectoryGeneratorExecutionControl
      .executeSequential(input)
  }
  
  def futureExec: Seq[ ETGQuery ] => Seq[ EducationalTrajectory ] = { input =>
    Await.result(
      GraphGenerator
        .EducationalTrajectoryGeneratorExecutionControl
        .executeInLocalThreads(input), 10.minute)
  }
  
  def testAndWrightLocal( out: PrintWriter, diGID: DiGID ): Unit = {
    val testData = generateQueries(diGID)
    //println("started")
    var line = Seq(testData.length,
                   testData.map(_.n).sum / testData.length,
                   testData.flatMap(_.s).sum,
                   testData.flatMap(_.k.map(_.sum)).sum
                   ).map(_.toString)
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
  
  def testAndWriteClusterOnly( out: PrintWriter, diGID: DiGID ): Unit = {
    val testData = generateQueries(diGID)
    //println("started")
    var line = Seq(testData.length,
                   testData.map(_.n).sum / testData.length,
                   testData.flatMap(_.s).sum,
                   testData.flatMap(_.k.map(_.sum)).sum
                   ).map(_.toString)
    tick()
    val r = clusterExec(testData)
    line = line.appended(tack.toString)
    out.println(line.mkString(","))
  }
  
  def infiniteVariationLocal(
                              init: DiGID,
                              variationGenerator: DiGID => ( Int => Int ) => LazyList[ DiGID ],
                              variationFunction: Int => Int,
                              filename: String
                            ): Unit = {
    val variations = variationGenerator(init)(variationFunction)
    val printer = new PrintWriter(filename)
    printer.println(s"Queries,number of sections,number of courses,number of KASes,Sequential,Local threads,Local " +
                      s"cluster")
    variations.foreach(testAndWrightLocal(printer, _))
  }
  
  def infiniteVariationCluster(
                                init: DiGID,
                                variationGenerator: DiGID => ( Int => Int ) => LazyList[ DiGID ],
                                variationFunction: Int => Int,
                                printer: PrintWriter
                              ): Unit = {
    val variations = variationGenerator(init)(variationFunction)
    printer.println(s"Queries,number of sections,number of courses,number of KASes,cluster")
    variations.foreach(testAndWriteClusterOnly(printer, _))
  }
  
  def infiniteConstVariationLocal =
    infiniteVariationLocal(initialDiGID, _, variateByConst(3), _)
  
  def infiniteDecilVariation =
    infiniteVariationLocal(initialDiGID, _, variateByDecil, _)
  
  def infiniteConstVariationCluster =
    infiniteVariationCluster(initialDiGID, _, variateByConst(3), _)
  
  ClusterInteractions.MasterInitialisation()
  //ждём, пока мастер придёт в себя
  sleep(5000)
  
  var curTime: Long = 0
  
  def tick( ): Unit = curTime = LocalTime.now().toNanoOfDay
  
  def tack = LocalTime.now().toNanoOfDay - curTime
  
  def output: Seq[ EducationalTrajectory ] => Unit = result =>
    println(result
              .map(_.sections
                    .map(_.courses
                          .mkString("\n"))
                    .mkString("\n\n"))
              .mkString(";\n\n\n"))
  
  def terminateAfter( min: Int )( code: => Unit ): Unit = Await.result(Future { code }, min.minutes) //seconds)
  
  def testAllLocal( ): Unit = {
    def ivc( vf: DiGID => ( Int => Int ) => LazyList[ DiGID ], fileName: String ): Unit =
      terminateAfter(30) {
        infiniteConstVariationLocal(vf, """C:\WorkData\Scala_Projects\ClusterAkk\src\main\resource\""" + fileName)
      }
    
    def ivcCluster( vf: DiGID => ( Int => Int ) => LazyList[ DiGID ], printer: PrintWriter ): Unit =
      terminateAfter(15) {
        infiniteConstVariationCluster(vf, printer)
      }
    
    def ivd( vf: DiGID => ( Int => Int ) => LazyList[ DiGID ], fileName: String ): Unit =
      terminateAfter(30) {
        infiniteDecilVariation(vf, """C:\WorkData\Scala_Projects\ClusterAkk\src\main\resource\""" + fileName)
      }
    
    val resource = """C:\WorkData\Scala_Projects\ClusterAkk\src\main\resource\"""
    
    def withPrinter( printerUser: PrintWriter => Unit, filename: String ) = {
      val printer = new PrintWriter(resource + filename)
      printerUser(printer)
      printer.close()
    }
    
    def executeClusterScenario( vf: DiGID => ( Int => Int ) => LazyList[ DiGID ], filename: String ) =
      withPrinter(printer => Try { ivcCluster(vf, printer) }, filename)
    
    //ivc(variateSections, "variateSectionsC.csv")
    //ivc(variateCourses,"variateCoursesC.csv")
    //ivc(variateIO,"variateIOC.csv")
    //ivc(variateQueries,"variateQueriesC.csv")
    val graphable = variateSections(initialDiGID)(variateByConst(1))
      .map(generateQueries) //.head
      
      .find(etgQuery => etgQuery.head.k
                                .map(_.length)
                                .sliding(2)
                                .forall(pair => pair.head > pair.last))
      .get
    
    
    val graph = GraphManager.graphmlFromET(seqExec(graphable).head)
    GraphManager
      .writeGraphML(
        graph,
        """C:\WorkData\Scala_Projects\Pet\Horizon\src\main\scala\xml\root4.graphml"""
        )
    
    //executeClusterScenario(variateSections,"variateSectionsCluster.csv")
    //executeClusterScenario(variateCourses,"variateCoursesCluster.csv")
    //executeClusterScenario(variateIO,"variateIOCluster.csv")
    //executeClusterScenario(variateQueries,"variateQueriesCluster.csv")
    
    throw new Exception
  }
  
  testAllLocal()
}
