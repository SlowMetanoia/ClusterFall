package MapReduce.own

import akka.actor.typed.Behavior
import akka.actor.typed.receptionist.{ Receptionist, ServiceKey }
import akka.actor.typed.scaladsl.{ Behaviors, Routers }
import akka.cluster.typed.Cluster

import scala.concurrent.ExecutionContext.global
import scala.concurrent.ExecutionContextExecutor

object Application {
  val executionContextSelection: ExecutionContextExecutor = global
  val NodeServiceKey: ServiceKey[NodeWorker.cmd ] = ServiceKey[NodeWorker.cmd]("Node")
  private object RootBehaviour{
    def apply():Behavior[Nothing] = Behaviors.setup[Nothing]{ ctx =>
      val cluster = Cluster(ctx.system)
      if (cluster.selfMember.hasRole("Local")){
        val node = ctx.spawn(NodeWorker(executionContextSelection),"Node")
        ctx.system.receptionist ! Receptionist.Register(NodeServiceKey, node)
      }
      if (cluster.selfMember.hasRole("Master")){
        val router = ctx.spawn(Routers.group(Application.NodeServiceKey),"Balancer")
        //ctx.spawn()
      }
      Behaviors.empty[Nothing]
    }
  }
}
