import scala.concurrent.ExecutionContext.global
import scala.concurrent.duration.Duration
import scala.concurrent.{ Await, Future }


val fut = Future{1*5}(global)
Await.result(fut, Duration(60,"sec"))

11/10
def variateByDecil:Int=>Int = n=> n + n/10 + 1
def variateInt: Int=> LazyList[Int] = n=>
  n#::variateInt(variateByDecil(n))
val ll = variateInt(3)

ll.take(15).force
