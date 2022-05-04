package MapReduce.own2

import akka.actor.typed.Behavior
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.typed.Cluster

import scala.concurrent.ExecutionContext.global
import scala.concurrent.ExecutionContextExecutor

object ApplicationSetup {
  val executionContextSelection: ExecutionContextExecutor = global
  val NodeServiceKey: ServiceKey[WorkItem[_,_]] = ServiceKey("MapReduceWorker")

  private object SetupNode {
    def apply(port:Int,ip:String): Behavior[ Nothing ] = Behaviors.setup[ Nothing ] { ctx =>
      val cluster = Cluster(ctx.system)
      val node = ctx.spawn(NodeWorker.setup(executionContextSelection), "Node")
      ctx.system.receptionist ! Receptionist.Register(NodeServiceKey, node)
      Behaviors.empty[ Nothing ]
    }
  }
  private object SetupMaster {
    def apply( port: Int, ip: String ): Behavior[ Nothing ] = Behaviors.setup[ Nothing ] { ctx =>
      val cluster = Cluster(ctx.system)
      val balancer = ctx.spawn(Balancer.setup(), "Balancer")
      val master = ctx.spawn(Master.setup(balancer), "Master")
      Behaviors.empty
    }
  }
  
  def main( args: Array[ String ] ): Unit = args match {
    case Array("master", port, ip) => SetupMaster(port.toInt ,ip)
    case Array("slave", port, ip) => SetupNode(port.toInt,ip)
    case Array("test") =>
      def sn(i:Int) = SetupNode(i,"127.0.0.1")
      def sm(i:Int) = SetupMaster(i,"127.0.0.1")
      for(i<-1 to 10) sn(25520+i)
      sm(25540)
    case _ => throw new IllegalArgumentException("wrong initializing arguments")
  }
}
