package MapReduce.own

import MapReduce.CborSerializable
import akka.actor.typed.ActorRef

import scala.collection.immutable
import scala.util.Try

object Master {
  sealed trait cmd extends CborSerializable
  sealed trait IFullTask extends cmd{
    type TIn
    type TOut
    val data:immutable.Iterable[TIn]
    val function:TIn=>TOut
    val reduce:(TOut,TOut)=>TOut
  }
  final case class FullTask[In,Out](data:immutable.Iterable[In],
                                    function:In=>Out,
                                    reduce:(Out,Out)=>Out,
                                    ) extends IFullTask {
    override type TIn = In
    override type TOut = Out
  }
  sealed trait IReply extends cmd {
    type TOut
    val localReducedAnswer: Try[TOut]
  }
  final case class Reply[Out]( localReducedAnswer: Try[Out] ) extends IReply{
    override type TOut = Out
  }
  
  final case class FinalAnswer[Out](value:Out,errors:Seq[(Throwable,ActorRef[NodeWorker.cmd])])
  final case class currentWorkers(workers:Set[ActorRef[NodeWorker.cmd]]) extends cmd
  /*
  def setup(
             workDivision: immutable.Iterable[_] => immutable.Iterable[Iterable[_]],
             messageQueueLength:Int
           ):Behavior[cmd] = Behaviors.setup[cmd]{ ctx=>
    Behaviors.receiveMessage{
      case ift:IFullTask =>
        awaitingForWorkers(
          workDivision(ift.data).map(workElements=>
                                                        NodeWorker.WorkPart(
                                                          workElements.map(_.asInstanceOf[ift.TIn]),
                                                          ift.function,
                                                          ift.reduce,
                                                          ctx.self)),
          messageQueueLength)
      case _ => throw new Exception("something went very wrong while idling")
    }
  }
  def awaitingForWorkers[In,Out]( workParts:Iterable[Iterable[NodeWorker.WorkPart[In,Out]]],
                                  messagesPerWorker:Int):Behavior[cmd] = Behaviors.setup[cmd]{ ctx=>
    Behaviors.receiveMessage{
      case currentWorkers(cw) => workInitialize(cw)
      case _ => throw new Exception("something went very wrong while awaiting for workers")
    }
  }
  def workInitialize(cw:Set[ActorRef[NodeWorker.cmd]]):Behavior[cmd] = Behaviors.setup[cmd]{ ctx=>
    for(i<-0 to messagesPerWorker)
      cw.foreach(_ ! )
  }*/
}
