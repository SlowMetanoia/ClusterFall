package MapReduce.SimpleSplit

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors

import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, Future }
import scala.language.postfixOps

object NodeWorker {
  def setup(implicit executionContext: ExecutionContext):Behavior[WorkItem[_,_]] = {
    Behaviors.setup[WorkItem[_,_]]{ctx =>
      Behaviors.receiveMessage{
        case WorkItem(data, func, reduceFunction, replyTo) =>
          //ctx.log.debug{ s"Work message ${WorkItem(data, func, reduceFunction, replyTo).hashCode()} accepted"}
          Future.reduceLeft{
            data.map{
              di=>
                Future(func(di))
            }
          }(reduceFunction).onComplete{ result =>
            replyTo ! Result(result.get,ctx.self)
          }
          Behaviors.same
      }
    }
  }

}
