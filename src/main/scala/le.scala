import com.typesafe.config.ConfigFactory

object le extends App {
  
  println(ConfigFactory.load("application"))
}
