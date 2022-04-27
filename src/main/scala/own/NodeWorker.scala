package own


import akka.actor.typed.{ ActorRef, Behavior }
import akka.actor.typed.scaladsl.Behaviors

import scala.collection.immutable
import scala.concurrent.{ ExecutionContext, Future }


object NodeWorker {
  sealed trait cmd extends CborSerializable
  sealed trait IWorkPart extends cmd{
    type TIn
    type TOut
    val data:immutable.Iterable[TIn]
    val function:TIn=>TOut
    val reduce:(TOut,TOut)=>TOut
    val replyTo:ActorRef[Master.cmd]
  }
  
  final case class WorkPart[In,Out](data:immutable.Iterable[In],
                                    function:In=>Out,
                                    reduce:(Out,Out)=>Out,
                                    replyTo:ActorRef[Master.cmd]) extends IWorkPart {
    override type TIn = In
    override type TOut = Out
  }
  final case class ExecutionContextSwitch( executionContext: ExecutionContext) extends cmd
  case object Stop extends cmd
  
  def apply(implicit executionContext: ExecutionContext):Behavior[cmd] = Behaviors.setup{ ctx =>
    Behaviors.receiveMessage {
      case wp: IWorkPart =>
        Future.reduceLeft {
          wp.data.map{ in =>
            Future(wp.function(in))
          }
        }(wp.reduce)
              .onComplete(result => wp.replyTo ! Master.Reply(result))
        Behaviors.same
      case Stop=>
        ctx.system.terminate()
        Behaviors.stopped
      case ExecutionContextSwitch(executionContext) =>
        NodeWorker(executionContext)
    }
  }
}
