import com.typesafe.config.ConfigFactory
import akka.actor._

object Master1System extends App {
	val system = ActorSystem("mastersystem1", ConfigFactory.load.getConfig("mastersystem1"))
}

object Master2System extends App {
	val system = ActorSystem("mastersystem2", ConfigFactory.load.getConfig("mastersystem2"))
}