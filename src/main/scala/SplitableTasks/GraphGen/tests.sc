import scala.annotation.tailrec

//Структуры данных
trait KAS{
  val id:Int
}
case class ExternalKAS(id:Int,name:String) extends KAS
trait ICourse{
  val id:Int
  val in:Set[KAS]
  val out:Set[KAS]
}
case class ExternalCourse(id:Int,name:String,in:Set[KAS],out:Set[KAS]) extends ICourse
case object EmptyCourse extends ICourse{
  override val id = null
  override val in = Set.empty
  override val out = Set.empty
}
case class Section(courses:Set[ICourse])
case class EducationalTrajectory(sections:Seq[Section]){
  def courses:Set[ICourse] = sections.flatMap(_.courses).toSet
  def KASes:Set[KAS] = sections.flatMap(_.courses.flatMap(c=> c.in ++ c.out)).toSet
}
case class CourseChain(courses:Seq[ICourse]){
  def length = courses.length
}

object EducationalTrajectoryGenerator{
  def generateET(n:Int,s:Seq[Int],k:Seq[Seq[Int]]):EducationalTrajectory = {
    abstractChains
      .andThen(_.map(ac=> generateChain(ac.toList,???,???)))
      //.andThen(expandChains)
      //.andThen(balanceChains)
      //.andThen(balanceTable)
      .andThen(tableFromChains(_,s))
      .andThen(releaseResult)(k)
  }
  
  def abstractChains:Seq[Seq[Int]]=>Seq[Seq[Int]] = sections =>
    for (j<-0 until sections.map(_.length).max ) yield
      for (i<-sections.indices if sections(i).length>j) yield sections(i)(j)

  def generateChain(
                     inputs:List[Int],
                     initialCourseGen:Int=>CourseChain,
                     sequentialCourseGen:(Int,CourseChain)=>CourseChain
                   ):CourseChain = {
    if(inputs.nonEmpty) {
      @tailrec
      def recGen( leftInputs: List[ Int ], chain: CourseChain ): CourseChain = leftInputs match {
        case Seq() => chain
        case h :: t => recGen(t, sequentialCourseGen(h, chain))
      }
      recGen(inputs.tail,initialCourseGen(inputs.head))
    } else
      CourseChain(Seq.empty)
  }
  def appendChains(l:Seq[CourseChain],r:Seq[CourseChain]):Seq[CourseChain] = l++r
  def expandChains:Seq[CourseChain]=>Seq[CourseChain] = { chains=>
    val n = chains.map(_.courses.length).max
    chains.map( chain =>
      CourseChain(
        for (i<-0 to n) yield
        if(i<chain.courses.length) chain.courses(i) else EmptyCourse
      )
    )
  }
  def balanceChains(chains:Seq[CourseChain]):Seq[Seq[ICourse]] = ???
  def balanceTable(table:Seq[Seq[ICourse]]):Seq[Seq[ICourse]] = ???
  def tableFromChains(chains:Seq[CourseChain],crsN:Seq[Int]):Seq[Seq[ICourse]] = {
    var flat  = chains.flatMap(_.courses)
    crsN.map{n=>
      val section = flat.take(n)
      flat = flat.drop(n)
      section
    }
  }
  def releaseResult(table:Seq[Seq[ICourse]]):EducationalTrajectory =
    EducationalTrajectory(table.map(cs=>Section(cs.toSet)))
}

val sections = Seq(
  Seq(1,2,3),
  Seq(4,5),
  Seq(6,7,8,9)
)

EducationalTrajectoryGenerator.abstractChains(sections).map(_.mkString(",")).mkString("\n")


sections.map(_.mkString(",")).mkString("\n")