package own

import akka.actor.{ Actor, Props }

class Slave extends Actor{
  override def receive: Receive = {
    case msg:String => println(msg)
  }
}
object Slave{
  def props():Props = {
    Props(new Slave)
  }
}
