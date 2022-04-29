package MapReduce.own

import MapReduce.CborSerializable
import akka.actor.typed.{ ActorRef, Behavior }
import akka.actor.typed.scaladsl.Behaviors

import scala.collection.immutable
import scala.concurrent.{ ExecutionContext, Future }


object NodeWorker {
  sealed trait cmd extends CborSerializable
  
  
  final case class WorkPart[In,Out]( data:immutable.Iterable[In],
                                     function:In=>Future[Out],
                                     reduceFunction:(Out,Out)=>Out,
                                     replyTo:ActorRef[Master.cmd]) extends cmd
  final case class ExecutionContextSwitch(executionContext: ExecutionContext) extends cmd
  case object Stop extends cmd
  
  def apply(implicit executionContext: ExecutionContext):Behavior[cmd] = Behaviors.setup{ ctx =>
    Behaviors.receiveMessage {
      case WorkPart(data, function, reduceFunction, replyTo) =>
        Future.reduceLeft {
          data.map{a=> function(a)}
        }(reduceFunction)
              .onComplete(result => replyTo ! Master.Reply(result))
        Behaviors.same
      case Stop=>
        ctx.system.terminate()
        Behaviors.stopped
      case ExecutionContextSwitch(executionContext) =>
        NodeWorker(executionContext)
    }
  }
}
