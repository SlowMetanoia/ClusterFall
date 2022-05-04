package MapReduce.own2

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors

object Master {
  def setup(balancer: ActorRef[Any]):Behavior[CDASCommand] = Behaviors.setup[CDASCommand]{ ctx=>
    Behaviors.receiveMessage{
      case MasterInit(data,f,rf,mf) =>
        var counter = 0
        mf(data).foreach{dp=>
          balancer ! WorkItem(dp,f,rf,balancer)
          counter+=1
        }
        reduce(balancer,counter,rf)
    }
  }
  def reduce[Out](balancer: ActorRef[Any],number:Int,rf:(Out,Out)=>Out):Behavior[CDASCommand] = Behaviors.setup[CDASCommand]{ ctx =>
    Behaviors.receiveMessage{
      case Result(result) => rf()
    }
  }
}
