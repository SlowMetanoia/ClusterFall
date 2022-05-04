package MapReduce.own2

import akka.actor.typed.Behavior
import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.scaladsl.Behaviors

object Balancer {
  def setup(): Behavior[Any] = Behaviors.setup[Any]{ctx=>
    ctx.system.receptionist ! Receptionist.Subscribe(ApplicationSetup.NodeServiceKey,ctx.self)
    ctx.system.receptionist ! Receptionist.Find(ApplicationSetup.NodeServiceKey,ctx.self)
    Behaviors.receiveMessage{
      case
    }
  }


}
