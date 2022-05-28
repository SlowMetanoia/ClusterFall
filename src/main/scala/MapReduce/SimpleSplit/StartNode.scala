package MapReduce.SimpleSplit

object StartNode extends App{
  ClusterInteractions.startup("Slave",25251,"192.168.1.8")
}
