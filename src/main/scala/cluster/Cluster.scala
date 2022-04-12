package cluster

object Cluster {
  case class Add(n1:Int,n2:Int)
  object BackendRegistration
  
  def main( args: Array[ String ] ): Unit = {
    Frontend.initiate()
    
    Backend.initiate(2560)
    Backend.initiate(2561)
    Backend.initiate(2562)
    
    Thread.sleep(10000)
    Frontend.getFrontend ! Add(2, 4)
  }
}