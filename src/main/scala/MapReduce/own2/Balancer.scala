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
        ctx.self ! wi
        prepareForWork(wi.replyTo, Map(0 -> rl))
    }
  }
  
  def prepareForWork(
                      master: ActorRef[ CDASCommand ],
                      actorsLoad: Map[ Int, Set[ Worker ] ],
                      storedMessages: Seq[ WorkItem[ _, _ ] ] = Seq.empty
                    ): Behavior[ Any ] = Behaviors.setup[ Any ] { ctx =>
    Behaviors.receiveMessage {
      case wi: WorkItem[ _, _ ] =>
        val (newActorsLoad, worker, minimal) = getMinimalLoaded(actorsLoad)
        if(minimal >= maxMessages)
          prepareForWork(master, actorsLoad, storedMessages.appended(wi))
        else {
          worker ! WorkItem(wi.data,wi.f,wi.fr,ctx.self)
          prepareForWork(master, newActorsLoad, storedMessages)
        }
      case result:Result[_] =>
        master ! result
        if(storedMessages.nonEmpty){
          //Todo
        }
    }
  }
  
  def getMinimalLoaded(
                        actorsLoad: Map[ Int, Set[ Worker ] ]
                      ): (Map[ Int, Set[ Worker ] ], Worker, Int) = {
    val minimal = actorsLoad.groupBy(_._2.nonEmpty)(true).keys.min
    val worker = actorsLoad(minimal).head
    (
      actorsLoad.updated(minimal, actorsLoad(minimal) - worker)
                .updated(minimal + 1, actorsLoad(minimal + 1) + worker),
      worker,
      minimal
    )
  }
}
