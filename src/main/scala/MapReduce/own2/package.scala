package MapReduce

import akka.actor.typed.ActorRef

import scala.collection.immutable

package object own2 {
  type Worker = ActorRef[WorkItem[_,_]]
  trait CDASCommand extends CborSerializable
  trait NodeWorkerCommand extends CDASCommand
  trait MasterCommand extends CDASCommand
  final case class MasterInit[In,Out](
                                       data:immutable.Iterable[In],
                                       f:In=>Out,
                                       rf:(Out,Out)=>Out,
                                       mf:immutable.Iterable[In]=>immutable.Iterable[immutable.Iterable[In]]
                                     ) extends MasterCommand
  final case class WorkItem[In,Out](data:immutable.Iterable[In],
                                    f:In=>Out,
                                    fr:(Out,Out)=>Out,
                                    replyTo:ActorRef[CDASCommand]) extends CDASCommand
  final case class Result[Out](outData:Out,worker:Worker) extends CDASCommand
  case object MessagesAreNoMore
}