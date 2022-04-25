package own

import akka.actor.{ ActorSystem, Props }
import com.typesafe.config.ConfigFactory

object LocalSystemStart extends App {
  
  val config = ConfigFactory.load("Local")
  val system = ActorSystem("local",config)
  val ref = system.actorOf(Props[Slave],"slave")
  ref ! "sdf"
}
