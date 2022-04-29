package MapReduce.own1

import akka.actor.typed.{ ActorRef, Behavior }
import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.scaladsl.Behaviors

object Master {
  def apply(receptionist: ActorRef[Receptionist.Listing],):Behavior[WorkMess] = Behaviors.setup{
    Behaviors.receiveMessage{
      case WorkPart =>
    }
  }
}
