package MapReduce.own2

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors

import scala.concurrent.Promise
import scala.util.Try

object Master {
  def setup(balancer: ActorRef[Any]):Behavior[CDASCommand] = Behaviors.setup[CDASCommand]{ ctx=>
    Behaviors.receiveMessage{
      case MasterInit(data,f,rf,mf,resultPlace) =>
        println("master inited")
        var counter = 0
        mf(data).foreach{dp=>
          balancer ! WorkItem(dp,f,rf,balancer)
          counter += 1
        }
        reduce(balancer,counter,rf, resultPlace = resultPlace)
    }
  }
  
  def reduce[Out](
                   balancer: ActorRef[Any],
                   number:Int,
                   rf:(Out,Out)=>Out,
                   value:Option[Out] = None,
                   resultPlace:Promise[Out]
                 ):Behavior[CDASCommand] = Behaviors.setup[CDASCommand]{ ctx =>
    println("reduce started")
    Behaviors.receiveMessage{
      case r:Result[Out] =>
        if(number>0)
          reduce(
            balancer,
            number,
            rf,
            reduceStep(rf,value,r.outData),
            resultPlace
          )
        else {
          balancer ! MessagesAreNoMore
          println("reduce ended")
          resultPlace.complete(Try{value.get})
          setup(balancer)
        }
    }
  }
  
  def reduceStep[T](rf:(T,T)=>T,oldV:Option[T],newV:T):Option[T] = oldV match {
      case Some(oldV) => Some(rf(oldV,newV))
      case None => Some(newV)
    }
  
}
