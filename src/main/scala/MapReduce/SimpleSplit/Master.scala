package MapReduce.SimpleSplit

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors

import scala.concurrent.Promise
import scala.util.Try

object Master {
  def setup( router: ActorRef[Any]):Behavior[CDASCommand] = Behaviors.setup[CDASCommand]{ ctx=>
    Behaviors.receiveMessage{
      case MasterInit(data,f,rf,mf,resultPlace) =>
        ctx.log.info("master inited")
        var counter = 0
        router ! RouterInit(ctx.self)
        mf(data).foreach{dp=>
          router ! WorkItem(dp, f, rf, router)
          counter += 1
        }
        reduce(router, counter, rf, resultPlace = resultPlace)
    }
  }
  
  def reduce[Out](
                   balancer: ActorRef[Any],
                   messagesLeft:Int,
                   rf:(Out,Out)=>Out,
                   value:Option[Out] = None,
                   resultPlace:Promise[Out]
                 ):Behavior[CDASCommand] = Behaviors.setup[CDASCommand]{ ctx =>
    ctx.log.debug(s"messages remain:$messagesLeft")
    if(messagesLeft > 0)
    Behaviors.receiveMessage{
      case r:Result[Out] =>
        reduce(
          balancer,
          messagesLeft -1,
          rf,
          reduceStep(rf,value,r.outData),
          resultPlace
          )
    }
    else {
      balancer ! MessagesAreNoMore
      ctx.log.info("reduce ended")
      resultPlace.complete(Try { value.get })
      setup(balancer)
    }
  }
  
  def reduceStep[T](rf:(T,T)=>T,oldV:Option[T],newV:T):Option[T] = oldV match {
      case Some(oldV) => Some(rf(oldV,newV))
      case None => Some(newV)
    }
}