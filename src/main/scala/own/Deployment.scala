package own

import akka.actor.{ ActorSystem, AddressFromURIString, Deploy, Props }
import akka.remote.RemoteScope
import com.typesafe.config.ConfigFactory

object Deployment extends App{
  val deploymentConfig = ConfigFactory.load("Deployment")
  val system = ActorSystem("DeploymentSystem",deploymentConfig)
  val port = ConfigFactory.load.getConfig("Local").getInt("akka.remote.artery.canonical.port")
  val ips = Seq("127.0.0.1")
  val uris = ips.map(ip=>s"akka.tcp://slave@$ip:$port")
  val addresses = uris.map(AddressFromURIString.apply)
  val allProps = addresses.map{adr=>
    Props[ Slave ].withDeploy(
      Deploy(scope = RemoteScope(adr))
      )
  }
  val refs = allProps.map(system.actorOf)
  //system.terminate()
  //fixme: actor path tells that actor is guarded by DeploymentSystem/user. Is shouldn`t be so. Research, what`s wrong.
  refs.foreach(ref=> println(ref.path))
  refs.foreach(ref => ref ! "Die, my darling!")
}
