import com.typesafe.config.ConfigFactory

println(ConfigFactory.load("application").getString("hostname"))
println(ConfigFactory.load("host").getString("hostname"))
println(ConfigFactory.load("host").getInt("DeploymentSystem.akka.remote.artery.canonical.port"))
println(ConfigFactory.load("host").getString("DeploymentSystem.akka.remote.artery.canonical.hostname"))