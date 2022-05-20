package MapReduce.SimpleSplit

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.Behavior

object WorkQueue {
  def withQ(q:List[CDASCommand] =List.empty):Behavior[CDASCommand] = Behaviors.setup[CDASCommand]{ctx =>
    Behaviors.receiveMessage{
      case mi:MasterInit[_,_] => withQ(q.appended(mi))
      case rdy(master) => if(q.nonEmpty) {
        master ! q.head
        withQ(q.tail)
        } else withQ(q)
    }
  }
}
