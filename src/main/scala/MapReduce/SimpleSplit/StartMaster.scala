package MapReduce.SimpleSplit

object StartMaster extends App{
  val master = ClusterInteractions.master
  ClusterInteractions.startup(
    "Master",
    52309,
    "127.0.0.1"
  )
}
