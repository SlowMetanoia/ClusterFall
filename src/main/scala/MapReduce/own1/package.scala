package MapReduce

import akka.actor.typed.ActorRef

import scala.collection.immutable
import scala.util.Try

package object own1 {
  sealed trait WorkMess
  final case class WorkPart[In,Out]( data:immutable.Iterable[In],
                                     handlingFunction:In=>Out,
                                     reduceFunction:(Out,Out)=>Out,
                                     nextLevelReducer:ActorRef[WorkMess]) extends WorkMess
  final case class WorkResult[Out](outputData:Try[Out]) extends WorkMess
}

