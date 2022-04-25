package own

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory

object LocalSystemStart extends App {
  val config = ConfigFactory.load("host").getConfig("Local")
  val system = ActorSystem("local",config)
}
