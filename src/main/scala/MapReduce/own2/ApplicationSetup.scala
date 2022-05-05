package MapReduce.own2

import akka.actor.typed.{ ActorSystem, Behavior }
import akka.actor.typed.receptionist.{ Receptionist, ServiceKey }
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.typed.Cluster
import com.typesafe.config.ConfigFactory

import scala.concurrent.ExecutionContext.global
import scala.concurrent.ExecutionContextExecutor

object ApplicationSetup {
  val executionContextSelection: ExecutionContextExecutor = global
  val NodeServiceKey: ServiceKey[ WorkItem[ _, _ ] ] = ServiceKey("MapReduceWorker")
  val MasterServiceKey: ServiceKey[CDASCommand] = ServiceKey("Master")
  
  private
  object RootBehaviour {
    def apply( ): Behavior[ Nothing ] = Behaviors.setup[ Nothing ] { ctx =>
      val cluster = Cluster(ctx.system)
      if(cluster.selfMember.hasRole("Slave")) {
        val node = ctx.spawn(NodeWorker.setup(executionContextSelection), "Node")
        ctx.system.receptionist ! Receptionist.Register(NodeServiceKey, node)
      }
      if(cluster.selfMember.hasRole("Master")) {
        val balancer = ctx.spawn(Balancer.setup(), "Balancer")
        val master = ctx.spawn(Master.setup(balancer), "Master")
        ctx.system.receptionist ! Receptionist.Register(MasterServiceKey, master)
      }
      Behaviors.empty[ Nothing ]
    }
  }
  
  def main( args: Array[ String ] ): Unit = args match {
    case Array(role, port, ip) => startup(role, port.toInt, ip)
    case Array("test") =>
      (1 to 3).foreach(i=> startup("Slave",25250+i))
      startup("Master",25539)
    case _ => throw new IllegalArgumentException("wrong initializing arguments")
  }
  
  def startup( role: String, port: Int, ip: String = "DEFAULT" ): ActorSystem[ Nothing ] = {
    val config = ConfigFactory
      .parseString(s"""
      hostname = ${ if(ip=="DEFAULT") "127.0.0.1" else ip}
      akka.remote.artery.canonical.port=$port
      akka.cluster.roles = [$role]
      """)
      .withFallback(ConfigFactory.load())
    println(ConfigFactory.load("application"))
    
    ActorSystem[ Nothing ](RootBehaviour(), "ClusterSystem", config)
  }
}
