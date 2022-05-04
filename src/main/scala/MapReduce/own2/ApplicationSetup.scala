package MapReduce.own2

import akka.actor.typed.Behavior
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.typed.Cluster

import scala.concurrent.ExecutionContext.global
import scala.concurrent.ExecutionContextExecutor

object ApplicationSetup {
  val executionContextSelection: ExecutionContextExecutor = global
  val NodeServiceKey = ServiceKey[ WorkItem[_,_] ]("MapReduceWorker")

  private object SetupNode {
    def apply(port:Int,role:String,ip:String): Behavior[ Nothing ] = Behaviors.setup[ Nothing ] { ctx =>
      val cluster = Cluster(ctx.system)
      val node = ctx.spawn(NodeWorker.setup(executionContextSelection), "Node")
      ctx.system.receptionist ! Receptionist.Register(NodeServiceKey, node)
      Behaviors.empty[ Nothing ]
    }
  }
}
