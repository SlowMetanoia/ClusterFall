import MapReduce.GraphGenerator._

type Table = Seq[ Seq[ Int ] ]
def rotateTable: Table => Table = table =>
  for (index <- 0 until table.map(_.length).max) yield
    for (vector <- table if vector.length > index) yield vector(index)


def generateLast: Seq[ Int ] => Seq[ Int ] = seq => seq.appended(seq.sum / seq.length)
def completeTable: Table => Table = _.map(generateLast)
def chainsInitFromTable: Table => Seq[ ChainInitialData ] = tbl =>
  tbl.scanLeft(ChainInitialData(0, 0, List.empty))(
    ( prevInit, inputs ) =>
      ChainInitialData(
        prevInit.courseIdInit + prevInit.inOuts.length,
        prevInit.kasIdInit + prevInit.inOuts.sum,
        inputs.toList
        )).tail
def generate: ChainInitialData => CourseChain = { chainInit =>
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

def sectionsFromChains:( Seq[ CourseChain ] )=> Seq[ Section ] = ( chains) => {
  (for (index <- 0 until chains.map(_.length).max) yield
    Section((for (chain <- chains if chain.length > index) yield chain.courses(index)).toSet))
  }
  
  

def ETFromSections: Seq[ Section ] => EducationalTrajectory = EducationalTrajectory


val testIn = Seq(
  Vector(6, 3, 3, 7, 3, 7),
  Vector(3, 5, 5, 4, 6, 3),
  Vector(4, 5, 7, 7, 5),
  Vector(6, 6, 7, 6),
  Vector(6, 5, 7, 4),
  Vector(3, 4, 5, 4, 7, 6),
  Vector(4, 6, 7, 5),
  Vector(3, 3, 6, 7)
  )
testIn.mkString("\n")
val release =
  rotateTable
    .andThen(completeTable)
    .andThen(chainsInitFromTable)
    .andThen(_.map(generate))
    .andThen(sectionsFromChains)
    .andThen(ETFromSections)

println(release(testIn))

