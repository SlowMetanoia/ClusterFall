package cluster

import akka.actor.{ Actor, ActorRef, ActorSystem, Props, Terminated }
import cluster.Cluster.{ Add, BackendRegistration }
import com.typesafe.config.ConfigFactory

class Frontend extends Actor{
  var backends = IndexedSeq.empty[ActorRef]
  
  def receive = {
    case Add if backends.isEmpty =>
      println("Service unreachable, cluster doesnt have backend node to handle it")
    case addOp:Add =>
      println("I'll forward this one to random backend node")
      backends(util.Random.nextInt(backends.length)) forward addOp
    case BackendRegistration if !backends.contains(sender()) =>
      backends = backends :+ sender()
      context watch sender()
    case Terminated(a) =>
      backends.filterNot(_ == a)
  }
}
object Frontend{
  private var _frontend:ActorRef = _
  
  def initiate(): Unit = {
    val config = ConfigFactory.load.getConfig("Frontend")
    val system = ActorSystem("ClusterSystem", config)
    _frontend = system.actorOf(Props[Frontend], name = "frontend")
  }
  def getFrontend: ActorRef = _frontend
}
