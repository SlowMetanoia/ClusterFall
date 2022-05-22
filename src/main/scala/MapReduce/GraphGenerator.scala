package MapReduce

import scala.collection.immutable.Iterable
import scala.concurrent.ExecutionContext.global
import scala.concurrent.Future

object GraphGenerator {
  //Структуры данных
  trait KAS {
    val id: Int
    override def toString: String = s"KAS($id)"
  }
  
  case class ExternalKAS( id: Int, name: String ) extends KAS
  
  case class InternalKAS( id: Int ) extends KAS
  
  trait ICourse {
    val id: Int
    val in: Set[ KAS ]
    val out: Set[ KAS ]
  
    override def toString: String =
      s"Course($id; IN:${in.toSeq.sortWith(_.id < _.id).mkString(",")}|#|" +
      s"OUT:${out.toSeq.sortWith(_.id < _.id).mkString(",")})"
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
    override val id = ???
    override val in = ???
    override val out = ???
  }
  
  case class Section( courses: Set[ ICourse ] )
  
  case
  class EducationalTrajectory( sections: Seq[ Section ] ) {
    def courses: Set[ ICourse ] = sections.flatMap(_.courses).toSet
    
    def KASes: Set[ KAS ] = sections.flatMap(_.courses.flatMap(c => c.in ++ c.out)).toSet
    
    def asGraph = ???
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
  
    def sectionsFromChains: Seq[ CourseChain ] => Seq[ Section ] =  chains => {
      (for (index <- 0 until chains.map(_.length).max) yield
        Section((for (chain <- chains if chain.length > index) yield chain.courses(index)).toSet))
    }
  
  
    def ETFromSections: Seq[ Section ] => EducationalTrajectory = EducationalTrajectory
  }
  
  object EducationalTrajectoryGeneratorExecutionControl {
    import EducationalTrajectoryGeneratorAPI._
    val messageUnitsLimit = 1
    
    def mapperFunction[ T ]: Iterable[ T ] => Iterable[ Iterable[ T ] ] =
      _.grouped(messageUnitsLimit).to(collection.immutable.Iterable.iterableFactory)
    
    def transformationFunction: ChainQuery => Seq[ ChainResponse ] = cq =>
      Seq(ChainResponse(EducationalTrajectoryGeneratorAPI.generateChain(cq.ci), cq.associatedET))
    
    def reduceFunction: ( Seq[ ChainResponse ] , Seq[ ChainResponse ]) => Seq[ ChainResponse ] = _ ++ _
    
    def preMap: Seq[ ETGQuery ] => Seq[ ChainQuery ] =
      _.zipWithIndex.flatMap(p =>
                             rotateTable
                               .andThen(completeRotatedTable)
                               .andThen(chainsInitFromRotatedTable)
                               .andThen( _.map(ChainQuery(_,p._2))
                                        )(p._1.k)
                             )
    
    def executeSequential: Seq[ ETGQuery ] => Seq[ EducationalTrajectory ] = queries =>
      preMap.andThen(mapperFunction)(queries)
            .flatMap(_.map(transformationFunction).reduce(reduceFunction))
            .groupBy(_.associatedET)
            .toSeq.sortWith(_._1 > _._1)
            .map(_._2.map(_.ci))
            .map(chains=>sectionsFromChains.andThen(ETFromSections)(chains.toSeq))
            
    
    def executeInCluster: Seq[ ETGQuery ] => Future[Seq[ EducationalTrajectory ]] = queries =>
      SimpleSplit.SplitExecution(
        preMap(queries), mapperFunction, reduceFunction, transformationFunction)
                 .map(_.groupBy(_.associatedET)
                       .toSeq
                       .sortWith(_._1 > _._1)
                       .toSeq.sortWith(_._1 > _._1)
                       .map(_._2.map(_.ci))
                       .map(chains=>sectionsFromChains.andThen(ETFromSections)(chains.toSeq))
              )(global)
    
  }
}
