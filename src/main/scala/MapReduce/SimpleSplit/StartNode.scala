package MapReduce.SimpleSplit

object StartNode extends App{
  ClusterInteractions.startup(
    "Slave",
    52250,
    "127.0.0.1"
    )
}
