package conf

import com.typesafe.config.{Config, ConfigFactory, ConfigValueFactory}

import scala.collection.JavaConversions._

object ClusterConfig {

  /**
    * This method given a class loader will return the configuration object for an ActorSystem
    * in a clustered environment
    *
    * @param classLoader the configured classloader of the application
    * @return Config
    */
  def loadConfig(classLoader: ClassLoader): Config = {
    val config = ConfigFactory.load(classLoader)

    val autoDiscovery = config.getBoolean("akka.cluster.auto-discovery")

    if (autoDiscovery) loadConfigWithAutoDiscovery(config)
    else config
  }

  private def loadConfigWithAutoDiscovery(config: Config): Config = {
    val ec2 = EC2ClientAPI()

    def getHost(): String = {
      ec2.getCurrentInstanceIp().get
    }

    def getSeeds(actorSystemName: String, port: Int): List[String] = {
      ec2.getSiblingInstancesIps().map { ips =>
        ips.map(ip => s"akka.tcp://$actorSystemName@$ip:$port")
      }.get
    }

    val actorSystemName = config.getString("play.akka.actor-system")
    val port = 2551 // Note: this port must be open in the EC2 instances

    val host = getHost()
    val seeds = getSeeds(actorSystemName, port)

    val overrideConfig =
      ConfigFactory.empty()
        .withValue("akka.remote.netty.tcp.hostname", ConfigValueFactory.fromAnyRef(host))
        .withValue("akka.remote.netty.tcp.port", ConfigValueFactory.fromAnyRef(port))
        .withValue("akka.cluster.seed-nodes", ConfigValueFactory.fromIterable(seeds))

    overrideConfig withFallback config
  }

}