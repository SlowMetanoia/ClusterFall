package MapReduce

import scala.collection.immutable.Iterable
import scala.concurrent.ExecutionContext.global
import scala.concurrent.Future

object GraphGenerator {
  //Структуры данных
  trait KAS {
    val id: Int
  }
  
  case class ExternalKAS( id: Int, name: String ) extends KAS
  
  case class InternalKAS( id: Int ) extends KAS
  
  trait ICourse {
    val id: Int
    val in: Set[ KAS ]
    val out: Set[ KAS ]
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
    
    def asGraph: Nothing = ???
  }
  
  case
  class CourseChain( courses: Seq[ ICourse ] ) {
    def length: Int = courses.length
  }
  
  case class ETGQuery( n: Int, s: Seq[ Int ], k: Seq[ Seq[ Int ] ] )
  
  case class ChainQuery( ci: ChainInitialData, associatedET: Int )
  
  case class ChainResponse( ci: CourseChain, associatedET: Int )
  
  object EducationalTrajectoryGeneratorAPI {
    def generateChainsInitialData: Seq[ Seq[ Int ] ] => Seq[ ChainInitialData ] = sections => {
      var curKASid = 0
      var curCourseId = 0
      //fixme some id wrong things are happening here (*)
      for (j <- 0 until sections.map(_.length).max) yield {
        val inOuts = for (i <- sections.indices
                          if sections(i).length > j) yield {
          curCourseId += 1
          curKASid += sections(i)(j)
          sections(i)(j)
        }
        val lco = inOuts.sum / inOuts.length
        curCourseId += 1
        curKASid += lco
        ChainInitialData(curCourseId, curKASid, inOuts.appended(lco).toList)
      }
    }
    
    def generateChain: ChainInitialData => CourseChain = courseInit => {
      var (cid, kid, _) = ChainInitialData.unapply(courseInit).get
      //fixme or here (*)
      def generateKASes( n: Int ): Set[ KAS ] = {
        val result = ( for (id <- kid until kid + n) yield InternalKAS(id) ).toSet
        kid += n
        //это бред полнейший, но иначе не работает.
        result.asInstanceOf[ Set[ KAS ] ]
      }
      
      def generateCourse( in: Set[ KAS ], out: Set[ KAS ] ): ICourse = {
        val result = InternalCourse(cid, in, out)
        cid += 1
        result
      }
      
      val in1 :: out1 :: outs = courseInit.inOuts.map(generateKASes)
      
      CourseChain(
        outs.scanLeft(generateCourse(in1, out1))(( course, outs ) => generateCourse(course.out, outs))
        )
    }
    
    def tableFromChains: (Seq[ CourseChain ], Seq[ Int ]) => Seq[ Seq[ ICourse ] ] = { ( chains, crsN ) =>
      var allCourses = chains.flatMap(_.courses)
      crsN.map { n =>
        val section = allCourses.take(n)
        allCourses = allCourses.drop(n)
        section
      }
    }
    
    def releaseResult( chains: Seq[ CourseChain ], csrN: Seq[ Int ] ): EducationalTrajectory =
      EducationalTrajectory(tableFromChains(chains, csrN).map(section => Section(section.toSet)))
  }
  
  object EducationalTrajectoryGeneratorExecutionControl {
    val messageUnitsLimit = 1
    
    def mapperFunction[ T ]: Iterable[ T ] => Iterable[ Iterable[ T ] ] =
      _.grouped(messageUnitsLimit).to(collection.immutable.Iterable.iterableFactory)
    
    def transformationFunction: ChainQuery => Seq[ ChainResponse ] = cq =>
      Seq(ChainResponse(EducationalTrajectoryGeneratorAPI.generateChain(cq.ci), cq.associatedET))
    
    def reduceFunction: ( Seq[ ChainResponse ] , Seq[ ChainResponse ]) => Seq[ ChainResponse ] = _ ++ _
    
    def preMap: Seq[ ETGQuery ] => Seq[   ChainQuery  ] =
      _.zipWithIndex.flatMap(p =>
                               EducationalTrajectoryGeneratorAPI
                                 .generateChainsInitialData(p._1.k)
                                 .map(ChainQuery(_, p._2))
                         )
    
    def executeSequential: Seq[ ETGQuery ] => Seq[ EducationalTrajectory ] = queries =>
      preMap.andThen(mapperFunction)(queries)
            .flatMap(_.map(transformationFunction).reduce(reduceFunction)).groupBy(_.associatedET)
            .toSeq.sortWith(_._1 > _._1)
            .map(_._2.map(_.ci))
            .zip(queries.map(_.s))
            .map(p => EducationalTrajectoryGeneratorAPI.releaseResult(p._1.toSeq, p._2))
            
    
    def executeInCluster: Seq[ ETGQuery ] => Future[Seq[ EducationalTrajectory ]] = queries =>
      
      //ClusterInteractions.MasterInitialisation()
      SimpleSplit.SplitExecution(
        preMap(queries), mapperFunction, reduceFunction, transformationFunction).map(_.groupBy(_.associatedET)
               .toSeq.sortWith(_._1 > _._1)
               .map(_._2.map(_.ci))
               .zip(queries.map(_.s))
               .map(p => EducationalTrajectoryGeneratorAPI.releaseResult(p._1, p._2))
              )(global)
    
  }
}
