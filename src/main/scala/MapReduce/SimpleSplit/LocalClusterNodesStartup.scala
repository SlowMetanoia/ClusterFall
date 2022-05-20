package MapReduce.SimpleSplit

object LocalClusterNodesStartup extends App {
  //Поднимаем ноды кластера локально
  ClusterInteractions.main(Array("test"))
}
