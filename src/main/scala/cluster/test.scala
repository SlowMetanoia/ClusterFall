package cluster

import com.typesafe.config.ConfigFactory

object test extends App{
  println(ConfigFactory.load.getConfig("MemberService"))
  println(ConfigFactory.load.getConfig("Backend"))
  println(ConfigFactory.load.getConfig("Frontend"))
}
