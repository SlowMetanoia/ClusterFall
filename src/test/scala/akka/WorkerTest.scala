package akka

import akka.actor.ActorSystem
import akka.testkit.TestKit
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike


class WorkerTest extends TestKit(ActorSystem("testSystem"))
                 with AnyWordSpecLike
                 with Matchers{
  "worker" should {
    val worker = system.actorOf(Worker.props())
    "apply functions to data" in{
      val f1:Int=>Int = _ + 1
      worker ! Worker.TaskMessage(1,f1,testActor)
      expectMsg(2)
      
      val f2:Int=>String = x=> (x * 2).toString
      worker ! Worker.TaskMessage(4,f2,testActor)
      expectMsg(8.toString)
    }
  }
}
