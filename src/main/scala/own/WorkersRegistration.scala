package own
import akka.actor.typed.{ ActorRef, Behavior }
import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.scaladsl.Behaviors

import scala.concurrent.ExecutionContext 

object WorkersRegistration {
  sealed trait cmd extends CborSerializable
  case class getCurrent(replyTo:ActorRef[Master.cmd]) extends cmd
  final case class ExecutionContextSwitch(executionContext:ExecutionContext) extends cmd
  
  def apply(workers:Set[ActorRef[NodeWorker.cmd]] = Set.empty): Behavior[Nothing] = Behaviors.setup{ ctx =>
    ctx.system.receptionist ! Receptionist.Subscribe(Application.NodeServiceKey,ctx.self)
    ctx.system.receptionist ! Receptionist.Find(Application.NodeServiceKey,ctx.self)
    Behaviors.receiveMessage{
      case Application.NodeServiceKey.Listing(listing) =>
        WorkersRegistration(workers = listing)
      case ExecutionContextSwitch(ecs) =>
        workers.foreach(_ ! NodeWorker.ExecutionContextSwitch(ecs))
        Behaviors.same
    }
  }
  def setup():Behavior[Nothing] = Behaviors.setup{ ctx =>
    WorkersRegistration()
  }
}
