package MapReduce.own2

import java.lang.Thread.sleep
import scala.concurrent.ExecutionContext.global
import scala.concurrent.Future
import scala.collection.immutable.Iterable

object testRun extends App{
  Future(ApplicationSetup.main(Array("test")))(global)
  sleep(20000)
  val result = Future(ApplicationSetup.splitExecution[Int,Int](
    data = 1 to 100,
    mf = mf[Int],
    f = {
      _ + 1},
    rf = _ + _
  ))(global)
  def mf[In]:Iterable[In]=>Iterable[Iterable[In]] = cc=> cc.map{el=>(1 to 10).map(_=>el)}

  result.flatten.onComplete(result => println(result))(global)
  sleep(10000)
}