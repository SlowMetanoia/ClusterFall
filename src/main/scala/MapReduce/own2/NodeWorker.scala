package MapReduce.own2

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors

import scala.concurrent.{ExecutionContext, Future}

object NodeWorker {
  def setup(implicit executionContext: ExecutionContext):Behavior[WorkItem[_,_]] = {
    Behaviors.setup[WorkItem[_,_]]{ctx =>
      Behaviors.receiveMessage{
        case WorkItem(data,f,fr,replyTo) =>
          Future.reduceLeft{
            data.map{
              di=>
                Future(f(di))
            }
          }(fr).onComplete(result=> replyTo ! Result(result,ctx.self))
          Behaviors.same
      }
    }
  }

}
