package MapReduce.own1

import MapReduce.CborSerializable
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ ActorRef, Behavior }

import scala.collection.immutable
import scala.concurrent.{ ExecutionContext, Future }


object NodeWorker {
  def apply(implicit executionContext: ExecutionContext):Behavior[WorkMess] = Behaviors.setup{ ctx =>
    Behaviors.receiveMessage {
      case WorkPart(data, function, reduce, replyTo) =>
        Future.reduceLeft{
          data.map(dataElem=>
                     Future(function(dataElem)))
        }(reduce).onComplete(replyTo ! WorkResult(_))
        Behaviors.same
    }
  }
}
