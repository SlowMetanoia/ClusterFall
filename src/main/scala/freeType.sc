import MapReduce.own2.WorkItem
import akka.actor.typed.ActorRef

import java.io.{FileInputStream, FileOutputStream, ObjectInputStream, ObjectOutputStream}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Promise
import scala.concurrent.duration.Duration
import scala.util.Try

trait message

case class Thr[A,B](a:A,b:B) extends message

case class Fut[A,B](a:A,f:A=>B) extends message

def c0:PartialFunction[message, Unit] = {
  case Thr(a,b) => false
  case fut:Fut[_,_] =>
    fut.f(fut.a)
}
def c1:PartialFunction[message,Unit] = {
  case Thr(a,b) => false
  case Fut(data,func) =>
    func(data)
}

c0(Thr(4,5))
c0(Fut(1,(a:Int)=> println(a + 1)) )

Iterable(1,3,5,25,6,7).min

val prms = Promise[Int]
prms.complete(Try{10})
prms.future.onComplete(res=> println(res))

/**
 * PHP => PHP Hates Programmers
 */

var al:Map[Int,Set[String]] = Map(0->Set("a","b","c","d"))
al = al + (1 -> Set("a")) + (0 -> (al(0) - "a"))
al

val f:Int=>Int = _ + 4

def fuckThisShitIAmOut[In,Out](f:In=>Out):In=>Out = {
  val out = new ObjectOutputStream(new FileOutputStream("/Users/alice/tmp/test.obj"))
  out.writeObject(f)
  out.close()

  val in = new ObjectInputStream(new FileInputStream("/Users/alice/tmp/test.obj"))
  val f0 = in.readObject().asInstanceOf[In => Out]
  in.close()
  f0
}

fuckThisShitIAmOut(f)(10)
