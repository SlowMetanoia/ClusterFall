package remoting

import akka.actor.{ ActorSystem, Props }
import com.typesafe.config.ConfigFactory
/*
* создали актора, почитали его путь
*/
object MemberService extends App{
  val config = ConfigFactory.load.getConfig("MemberService")
  
  val system = ActorSystem("MemberService", config)
  
  val worker = system.actorOf(Props[Worker], "remoteWorker")
  
  println(s"remoteWorker actor path is ${worker.path}")
}
/*
* Постучались в актора удалённо. Не получилось, ибо само приложение открывается на порте 25520 и открыть 2 не получается.
* Настройку порта, на котором запускается приложение я не нашёл.
*/
object MemberServiceLookUp extends App{
  val config = ConfigFactory.load.getConfig("MemberServiceLookUp")
  val system = ActorSystem("MemberServiceLookUp", config)
  val worker = system.actorSelection("akka.tcp://MemberService@127.0.0.1:2552/user/remote-worker")
  worker ! Worker.Work("yep, this is MY message!")
}

/*
* Удалённо создали актора.
* Кинули ему сообщение и удостоверились, что всё работает.
*/
object MemberServiceRemoteCreation extends App{
  val config = ConfigFactory.load.getConfig("MemberServiceRemoteCreation")
  val system = ActorSystem("MemberService", config)
  val workerActor = system.actorOf(Props[Worker], "workerActorRemote")
  println(s"remote worker path is ${workerActor.path}")
  
  workerActor ! Worker.Work("Hi, newbie")
}