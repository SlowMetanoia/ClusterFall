package akka

import akka.Worker.ITaskMessage
import akka.actor.{ Actor, ActorRef, Props }

class Worker extends Actor{
  override def receive: Receive = {
    case taskMessage:ITaskMessage =>
      taskMessage.replyTo ! taskMessage.function(taskMessage.data)
      //todo: define new exception in object and replace this one.
    case _=> throw new Exception("Worker got wrong data")
  }
}

object Worker{
  sealed trait ITaskMessage{
    type TIn
    type TOut
    val data: TIn
    val function: TIn=>TOut
    val replyTo: ActorRef
  }
  case class TaskMessage[In,Out](
                                  data:In,
                                  function:In=>Out,
                                  replyTo:ActorRef) extends ITaskMessage{
    override type TIn = In
    override type TOut = Out
  }
  def props():Props =
    Props(new Worker)
}
