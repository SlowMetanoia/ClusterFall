package MapReduce.own1

import akka.actor.typed.Behavior
import akka.actor.typed.receptionist.{ Receptionist, ServiceKey }
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.typed.Cluster

import scala.concurrent.ExecutionContext.global
import scala.concurrent.ExecutionContextExecutor

object NodeStart {
  val executionContextSelection: ExecutionContextExecutor = global
  val NodeServiceKey = ServiceKey[ WorkMess ]("MapReduceWorker")
  
  private object SetupNode {
    def apply( ): Behavior[ Nothing ] = Behaviors.setup[ Nothing ] { ctx =>
      val cluster = Cluster(ctx.system)
      val node = ctx.spawn(NodeWorker(executionContextSelection), "Node")
      ctx.system.receptionist ! Receptionist.Register(NodeServiceKey, node)
      Behaviors.empty[ Nothing ]
    }
  }
  private object SetupMaster {
  
  }
}
