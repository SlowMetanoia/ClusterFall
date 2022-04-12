package akka

import akka.actor.ActorRef

case class WorkerTask[In,Out](data:In, f:In=>Out, replyTo:ActorRef){}
