import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object freeType extends App{
  trait message
  
  case class Fut[A,B](a:A,f:Function[A,B]) extends message
  def c0:PartialFunction[message, Unit] = {
    case Fut(a,f) =>
      Future{f(a)}.onComplete(println)
  }
  
  c0(Fut(1, ( a: Int ) => a + 1))
}
