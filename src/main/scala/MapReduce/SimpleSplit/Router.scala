package MapReduce.SimpleSplit

import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ ActorRef, Behavior }


object Router {
  val maxMessages = 3
  def setup: Behavior[ Any ] = Behaviors.setup[ Any ] { ctx =>
    ctx.system.receptionist ! Receptionist.Subscribe(ClusterInteractions.NodeServiceKey, ctx.self)
    ctx.system.receptionist ! Receptionist.Find(ClusterInteractions.NodeServiceKey, ctx.self)
    waitingState()
  }
  def waitingState(rl: Set[ ActorRef[ WorkItem[ _, _ ] ] ] = Set.empty ): Behavior[ Any ] = Behaviors.setup[ Any ] { ctx =>
    Behaviors.receiveMessage {
      case rl: Receptionist.Listing =>
        waitingState(rl.serviceInstances(ClusterInteractions.NodeServiceKey))
      case RouterInit(master) =>
        if(rl.nonEmpty) {
          ctx.log.info(s"work inited with nodes:\n${ rl.mkString("\n") }")
          workingState(master, actorsQueue = for (i <- 1 to maxMessages; j <- rl) yield j)
        } else throw new NoWorkersException
    }
  }
  def workingState(
                    master: ActorRef[ CDASCommand ],
                    actorsQueue: Seq[Worker],
                    messagesQueue: Seq[ WorkItem[ _, _ ] ] = Seq.empty
                  ): Behavior[ Any ] = Behaviors.setup[ Any ] { ctx =>
    ctx.log.debug(s"now aql is ${actorsQueue.length}")
    ctx.log.debug(s"now mql is ${messagesQueue.length}")

    if(actorsQueue.nonEmpty & messagesQueue.nonEmpty){
      ctx.log.debug("work sent")
      actorsQueue.head ! messagesQueue.head
      workingState(master, actorsQueue.tail, messagesQueue.tail)
    } else
    Behaviors.receiveMessage {
      case wi: WorkItem[ _, _ ] =>
        workingState(master,actorsQueue,messagesQueue.appended(wi))
      case result: Result[ _ ] =>
        ctx.log.debug("result got")
        master ! result
        workingState(master,actorsQueue.appended(result.worker),messagesQueue)
      case _:Receptionist.Listing => Behaviors.same
      case MessagesAreNoMore => waitingState(actorsQueue.toSet)
    }
  }
}
