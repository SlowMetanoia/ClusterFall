package MapReduce.SimpleSplit

import java.lang.Thread.sleep
import scala.collection.immutable.Iterable
import scala.concurrent.ExecutionContext.global

object testRun extends App{
  //входные данные
  val data = 1 to 50
  def mf[In]:Iterable[In]=>Iterable[Iterable[In]] = cc => cc.map{el=>(1 to 3).map(_=>el)}
  def rf(a:Int,b:Int):Int = a + b
  def f:Int=>Int = _ + 1
  
  println(mf( data ).map(_.map(f).reduce(rf)).reduce(rf))

  LocalClusterStartup.main(Array.empty)
  sleep(5000)
  //APIPA
  val result = SplitExecution(data, mf, rf, f)
  val result2 = SplitExecution(data, mf, rf, f)
  //вывод
  result.onComplete(result => println(result))(global)
  result2.onComplete(result => println(result))(global)
  sleep(10000)
}
