package MapReduce.own1

import akka.actor.typed.Behavior
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.receptionist.Receptionist.Subscribe
import akka.actor.typed.scaladsl.Behaviors

object WorkersReceptionist {
  case object GiveMeWorkers
  def apply():Behavior[Any] = Behaviors.setup{
    Behaviors.receiveMessage{
      case NodeStart.NodeServiceKey.Listing(listing)=>
        Behaviors.same
    }
  }
  def setup:Behavior[Any] = Behaviors.setup{ ctx =>
    ctx.system.receptionist ! Receptionist.Subscribe(NodeStart.NodeServiceKey,ctx.self)
    ctx.system.receptionist ! Receptionist.Find(NodeStart.NodeServiceKey,ctx.self)
    WorkersReceptionist()
  }
}
