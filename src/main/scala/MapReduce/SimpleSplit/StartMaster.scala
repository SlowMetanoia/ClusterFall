package MapReduce.SimpleSplit

object StartMaster extends App{
  val master = SplitExecution.master
  SplitExecution.startup(
    "Master",
    52309,
    "127.0.0.1"
  )
}
