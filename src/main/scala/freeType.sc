import scala.concurrent.ExecutionContext.global
import scala.concurrent.duration.Duration
import scala.concurrent.{ Await, Future }


val fut = Future{1*5}(global)
Await.result(fut, Duration(60,"sec"))
