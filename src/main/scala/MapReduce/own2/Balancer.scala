package MapReduce.own2

import akka.actor.typed.{ ActorRef, Behavior }
import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.scaladsl.Behaviors

object Balancer {
  val maxMessages = 3
  
  def setup( rl: Set[ ActorRef[ WorkItem[ _, _ ] ] ] = Set.empty ): Behavior[ Any ] = Behaviors.setup[ Any ] { ctx =>
    ctx.system.receptionist ! Receptionist.Subscribe(ApplicationSetup.NodeServiceKey, ctx.self)
    ctx.system.receptionist ! Receptionist.Find(ApplicationSetup.NodeServiceKey, ctx.self)
    Behaviors.receiveMessage {
      case rl: Receptionist.Listing =>
        setup(rl.serviceInstances(ApplicationSetup.NodeServiceKey))
      case wi: WorkItem[ _, _ ] =>
        println(s"work inited with $rl")
        ctx.self ! wi
        prepareForWork(wi.replyTo, Map(0 -> rl))

    }
  }
  
  def prepareForWork(
                      master: ActorRef[ CDASCommand ],
                      actorsLoad: Map[ Int, Set[ Worker ] ],
                      storedMessages: Seq[ WorkItem[ _, _ ] ] = Seq.empty
                    ): Behavior[ Any ] = Behaviors.setup[ Any ] { ctx =>
    println("balancer is up")
    Behaviors.receiveMessage {
      case wi: WorkItem[ _, _ ] =>
        val (newActorsLoad, worker, minimal) = increasedMinimalLoaded(actorsLoad)
        if(minimal >= maxMessages)
          prepareForWork(master, actorsLoad, storedMessages.appended(wi))
        else {
          worker ! WorkItem(wi.data, wi.f, wi.fr, ctx.self)
          prepareForWork(master, newActorsLoad, storedMessages)
        }
      case result: Result[ _ ] =>
        master ! result
        if(storedMessages.nonEmpty) {
          result.worker ! storedMessages.head
          prepareForWork(master, actorsLoad, storedMessages.tail)
        }
        else prepareForWork(master, decreasedWorkerLoad(actorsLoad,result.worker), storedMessages)
      case _:Receptionist.Listing => Behaviors.same
      case MessagesAreNoMore => setup(actorsLoad.values.toSet.flatten)
    }
  }
  
  def increasedMinimalLoaded(
                              actorsLoad: Map[ Int, Set[ Worker ] ]
                            ): (Map[ Int, Set[ Worker ] ], Worker, Int) = {
    val minimal = actorsLoad.groupBy(_._2.nonEmpty)(true).keys.min
    val worker = actorsLoad(minimal).head
    (
      actorsLoad + (minimal -> (actorsLoad(minimal) - worker)) +
        (minimal + 1 -> (actorsLoad.getOrElse(minimal + 1,Set.empty[Worker]) + worker)),
      worker,
      if(actorsLoad(minimal).tail.nonEmpty) minimal else minimal + 1
    )
  }
  
  def decreasedWorkerLoad(
                           actorsLoad: Map[ Int, Set[ Worker ] ],
                           worker: Worker
                         ): Map[ Int, Set[ Worker ] ] = {
    val load = actorsLoad.find(_._2.contains(worker)).get._1
    actorsLoad + (load-> (actorsLoad(load) - worker)) +
      (load - 1 -> (actorsLoad(load) + worker))
  }
}
