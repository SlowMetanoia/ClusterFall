package remoting

import akka.actor.Actor


class Worker extends Actor{
  import Worker._
  override def receive: Receive = {
    case msg:Work =>
      println(s"I received work message $msg and my actorRef is $self")
  }
}
object Worker{
  case class Work(message:String)
}