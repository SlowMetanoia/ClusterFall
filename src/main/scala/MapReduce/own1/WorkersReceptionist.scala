package MapReduce.own1
/*
object WorkersReceptionist {
  case object GiveMeWorkers
  def apply():Behavior[Any] = Behaviors.setup[Any]{
    Behaviors.receiveMessage{
      case NodeStart.NodeServiceKey.Listing(listing)=>
        Behaviors.same
    }
    Behaviors.same
  }
  def setup:Behavior[Any] = Behaviors.setup{ ctx =>
    ctx.system.receptionist ! Receptionist.Subscribe(NodeStart.NodeServiceKey,ctx.self)
    ctx.system.receptionist ! Receptionist.Find(NodeStart.NodeServiceKey,ctx.self)
    WorkersReceptionist()
  }
}
*/