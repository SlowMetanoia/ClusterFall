package MapReduce.own1

import akka.actor.typed.Behavior
import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.receptionist.Receptionist.Subscribe
import akka.actor.typed.scaladsl.Behaviors

object WorkersReceptionist {
  def apply:Behavior[Receptionist.Listing] = Behaviors.setup{
  
  }
  def setup:Behavior[Receptionist.Listing] = Behaviors.setup{ ctx =>
    ctx.system.receptionist ! Receptionist.Subscribe(NodeStart.NodeServiceKey,ctx.self)
    ctx.system.receptionist ! Receptionist.Find(NodeStart.NodeServiceKey,ctx.self)
  }
}
