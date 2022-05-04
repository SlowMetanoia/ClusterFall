import MapReduce.own2.WorkItem
import akka.actor.typed.ActorRef

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
c0(Fut(1,(a:Int)=> println((a + 1))) )

Iterable(1,3,5,25,6,7).min

def startWork(actorsLoad:Map[Int,Set[ActorRef[WorkItem[_,_]]]]): Int = {
      val minimum = actorsLoad.keySet.min
      val worker = actorsLoad(minimum).head
  }