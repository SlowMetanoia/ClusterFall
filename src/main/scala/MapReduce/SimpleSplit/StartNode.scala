package MapReduce.SimpleSplit

object StartNode extends App{
  SplitExecution.startup(
    "Slave",
    52250,
    "127.0.0.1"
    )
}
