package MapReduce

import scala.collection.immutable.Iterable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.xml.{ Elem, XML }

object GraphGenerator {
  
  trait GraphNode {
    val id: Int
  }
  
  //Структуры данных
  trait KAS extends GraphNode {
    val id: Int
    
    override def toString: String = s"KAS($id)"
  }
  
  case class ExternalKAS( id: Int, name: String ) extends KAS
  
  case class InternalKAS( id: Int ) extends KAS
  
  trait ICourse extends GraphNode {
    val id: Int
    val in: Set[ KAS ]
    val out: Set[ KAS ]
    
    override
    def toString: String =
      s"Course($id; IN:${ in.toSeq.sortWith(_.id < _.id).mkString(",") }|#|" +
        s"OUT:${ out.toSeq.sortWith(_.id < _.id).mkString(",") })"
  }
  
  case
  class ExternalCourse(
                        id: Int, name: String,
                        in: Set[ KAS ],
                        out: Set[ KAS ]
                      ) extends ICourse
  
  case
  class InternalCourse(
                        id: Int,
                        in: Set[ KAS ], out: Set[ KAS ]
                      ) extends ICourse
  
  case
  class ChainInitialData(
                          courseIdInit: Int,
                          kasIdInit: Int,
                          inOuts: List[ Int ]
                        )
  
  case
  object EmptyCourse extends ICourse {
    override val id: Int = -1
    override val in: Set[ KAS ] = Set.empty
    override val out: Set[ KAS ] = Set.empty
  }
  
  case class Section( courses: Set[ ICourse ] )
  
  case
  class EducationalTrajectory( sections: Seq[ Section ] ) {
    def courses: Set[ ICourse ] = sections.flatMap(_.courses).toSet
    
    def KASes: Set[ KAS ] = sections.flatMap(_.courses.flatMap(c => c.in ++ c.out)).toSet
    
    def asGraph( ): Unit = {}
  }
  
  case
  class CourseChain( courses: Seq[ ICourse ] ) {
    def length: Int = courses.length
  }
  
  case class ETGQuery( n: Int, s: Seq[ Int ], k: Seq[ Seq[ Int ] ] )
  
  case class ChainQuery( ci: ChainInitialData, associatedET: Int )
  
  case class ChainResponse( ci: CourseChain, associatedET: Int )
  
  object EducationalTrajectoryGeneratorAPI {
    type Table = Seq[ Seq[ Int ] ]
    
    def rotateTable: Table => Table = table =>
      for (index <- 0 until table.map(_.length).max) yield
        for (vector <- table if vector.length > index) yield vector(index)
    
    def generateLast: Seq[ Int ] => Seq[ Int ] = seq => seq.appended(seq.sum / seq.length)
    
    def completeRotatedTable: Table => Table = _.map(generateLast)
    
    def chainsInitFromRotatedTable: Table => Seq[ ChainInitialData ] = tbl =>
      tbl.scanLeft(ChainInitialData(0, 0, List.empty))(
        ( prevInit, inputs ) =>
          ChainInitialData(
            prevInit.courseIdInit + prevInit.inOuts.length,
            prevInit.kasIdInit + prevInit.inOuts.sum,
            inputs.toList
            )).tail
    
    def generateChain: ChainInitialData => CourseChain = { chainInit =>
      var (courseId, kasId) = (chainInit.courseIdInit, chainInit.kasIdInit)
      
      def generateKASes( n: Int ): Seq[ KAS ] = {
        val result = for (id <- kasId until kasId + n) yield InternalKAS(id)
        kasId += n
        result
      }
      
      def generateCourse( in: Set[ KAS ], out: Set[ KAS ] ): ICourse = {
        val result = InternalCourse(courseId, in, out)
        courseId += 1
        result
      }
      
      val in1 :: out1 :: tail = chainInit.inOuts.map(generateKASes)
      CourseChain(tail.scanLeft(
        generateCourse(in1.toSet, out1.toSet)
        )(
        ( prevCourse, newKASes ) =>
          generateCourse(prevCourse.out, newKASes.toSet)
        ))
    }
    
    def sectionsFromChains: Seq[ CourseChain ] => Seq[ Section ] = chains => {
      for (index <- 0 until chains.map(_.length).max) yield
        Section(( for (chain <- chains if chain.length > index) yield chain.courses(index) ).toSet)
    }
    
    
    def ETFromSections: Seq[ Section ] => EducationalTrajectory = EducationalTrajectory
    
    def ET2Chains: EducationalTrajectory => Seq[ Seq[ ICourse ] ] = { et =>
      val table = et.sections.map(_.courses.toSeq)
      for (index <- 0 until table.map(_.length).max) yield
        for (vector <- table if vector.length > index) yield vector(index)
    }
  }
  
  object EducationalTrajectoryGeneratorExecutionControl {
    
    import EducationalTrajectoryGeneratorAPI._
    
    val messageUnitsLimit = 1
    
    def mapperFunction[ T ]: Iterable[ T ] => Iterable[ Iterable[ T ] ] =
      _.grouped(messageUnitsLimit).to(collection.immutable.Iterable.iterableFactory)
    
    def transformationFunction: ChainQuery => Seq[ ChainResponse ] = cq =>
      Seq(ChainResponse(EducationalTrajectoryGeneratorAPI.generateChain(cq.ci), cq.associatedET))
    
    def reduceFunction: (Seq[ ChainResponse ], Seq[ ChainResponse ]) => Seq[ ChainResponse ] = _ ++ _
    
    def preMap: Seq[ ETGQuery ] => Seq[ ChainQuery ] =
      _.zipWithIndex.flatMap(p =>
                               rotateTable
                                 .andThen(completeRotatedTable)
                                 .andThen(chainsInitFromRotatedTable)
                                 .andThen(_.map(ChainQuery(_, p._2))
                                          )(p._1.k)
                             )
    
    def educationalTrajectoryFromResponses: Iterable[ ChainResponse ] => Seq[ EducationalTrajectory ] = { responses =>
      responses.groupBy(_.associatedET)
               .toSeq
               .sortWith(_._1 > _._1)
               .map(_._2.map(_.ci))
               .map(chains => sectionsFromChains.andThen(ETFromSections)(chains.toSeq))
    }
    
    def executeSequential: Seq[ ETGQuery ] => Seq[ EducationalTrajectory ] = queries =>
      preMap.andThen(mapperFunction)
            .andThen(
              _.flatMap(
                _.map(transformationFunction)
                 .reduce(reduceFunction)
                )
              )
            .andThen(educationalTrajectoryFromResponses)(queries)
    
    def executeInCluster: Seq[ ETGQuery ] => Future[ Seq[ EducationalTrajectory ] ] = queries =>
      SimpleSplit.SplitExecution(
        preMap(queries), mapperFunction, reduceFunction, transformationFunction)
                 .map(educationalTrajectoryFromResponses)
    
    def executeInLocalThreads: Seq[ ETGQuery ] => Future[ Seq[ EducationalTrajectory ] ] = queries =>
      Future.reduceLeft {
        preMap
          .andThen(mapperFunction)(queries)
          .flatten
          .map { query =>
            Future { transformationFunction(query) }
          }
      }(reduceFunction).map(educationalTrajectoryFromResponses)
  }
  
  object GraphManager {
    case
    class CoursePlug( id: Int ) extends GraphNode {
      override def toString: String = s"Course($id)"
    }
    
    def graphmlFromET: EducationalTrajectory => Elem = { et =>
      val nodes = et
        .courses
        .flatMap(course =>
                   Seq(CoursePlug(course.id)) ++
                     course.in.toSeq ++
                     course.out.toSeq
                 )
        .map(node => <node id={node.toString}/>)
      
      val edges = EducationalTrajectoryGeneratorAPI
        .ET2Chains(et)
        .flatMap(
          _.flatMap { course =>
            course.in.map(kasIn => <edge
              id={kasIn.toString + CoursePlug(course.id).toString}
              source={kasIn.toString}
              target={CoursePlug(course.id).toString}/>) ++
              course.out.map(kasOut => <edge
                id={CoursePlug(course.id).toString + kasOut.toString}
                source={CoursePlug(course.id).toString}
                target={kasOut.toString}/>)
          })
      <graphml xmlns="http://graphml.graphdrawing.org/xmlns"
               xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
               xsi:schemaLocation="http://graphml.graphdrawing.org/xmlns/1.0/graphml.xsd">
        <graph id="G" edgedefault="directed">
          {nodes}{edges}
        </graph>
      </graphml>
    }
    
    def writeGraphML( el: Elem, filename: String ): Unit = XML.save(filename, el)
    
    def writeETasGraphML( et: EducationalTrajectory, filename: String ): Unit =
      writeGraphML(graphmlFromET(et), filename)
  }
}
