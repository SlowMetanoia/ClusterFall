
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
