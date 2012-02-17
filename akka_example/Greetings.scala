import akka.actor._

class Greetings extends Actor {
	def receive ={
		case name => println("Hello " + name)
	}
}

object GreetingsApp extends App {
	val greetingSystem = ActorSystem("greetings_system")
	val actor = greetingSystem.actorOf(Props[Greetings])
	actor ! "Nilanjan"	
}
