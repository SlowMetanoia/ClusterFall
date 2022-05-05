package MapReduce.own1
/*
object Master {
  def apply(mapReduceContext:MapReduceContext):Behavior[WorkMess] = Behaviors.setup{
    Behaviors.receiveMessage{
      case wp:WorkPart[_,_] =>
        awaitingForWorkers(wp,mapReduceContext)
      case _ => throw new Exception("something went wrong in Master state")
    }
    def awaitingForWorkers[In,Out](wp:WorkPart[In,Out], mapReduceContext: MapReduceContext):Behavior[WorkersList] = {
      Behaviors.receiveMessage{
        case WorkersList(wl) =>
        case _ => throw new Exception("something went wrong in Master state")
      }
    }
    def initialization[In,Out] (wp:WorkPart[In,Out],
                                mapReduceContext: MapReduceContext):Behavior[InitializationDone.type] = {
      case InitializationDone =>
    }
  }
}
*/