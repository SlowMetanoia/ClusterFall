package MapReduce.SimpleSplit

import java.lang.Thread.sleep
import scala.collection.immutable.Iterable
import scala.concurrent.ExecutionContext.global
import scala.concurrent.Future

object testRun extends App{
  val data = 1 to 50
  def mf[In]:Iterable[In]=>Iterable[Iterable[In]] = cc=> cc.map{el=>(1 to 3).map(_=>el)}
  def rf(a:Int,b:Int):Int = a + b
  def f(a:Int):Int = a + 1
  println(mf( data ).map(_.map(f).reduce(rf)).reduce(rf))
  Future(SplitExecution.main(Array("test")))(global)
  val result = Future(SplitExecution.start[Int,Int](
    data = testRun.data,
    mf = mf[Int],
    f = f,
    rf = rf
  ))(global)
  
  result.flatten.onComplete(result => println(result))(global)
  sleep(1000)
}