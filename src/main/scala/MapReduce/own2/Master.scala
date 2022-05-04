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
          counter += 1
        }
        balancer ! MessagesAreNoMore
        reduce(balancer,counter,rf)
    }
  }
  
  def reduce[Out](balancer: ActorRef[Any],
                  number:Int,rf:(Out,Out)=>Out,
                  value:Option[Out] = None):Behavior[CDASCommand] = Behaviors.setup[CDASCommand]{ ctx =>
    Behaviors.receiveMessage{
      case r:Result[Out] =>
        if(number>0)
          reduce(
          balancer,
          number,
          rf,
          reduceStep(rf,value,r.outData)
          )
        else {
          // код для возврата значения, возможно через сайд-эффект
          setup(balancer)
        }
    }
  }
  
  def reduceStep[T](rf:(T,T)=>T,oldV:Option[T],newV:T):Option[T] = oldV match {
      case Some(oldV) => Some(rf(oldV,newV))
      case None => Some(newV)
    }
  
}
