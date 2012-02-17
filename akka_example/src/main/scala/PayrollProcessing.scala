import akka.actor._
import akka.routing.RoundRobinRouter
import scala.io.Source
import com.typesafe.config.ConfigFactory
import akka.routing._

case class PayrollFile(fileName: String)
case class PayrollChunk(payload: Seq[String])
case class SinglePayroll(employeeId: String, baseSalary: BigDecimal, daysOff: Int, perks: BigDecimal)
case object WorkerDone
case object MasterDone

class WorkerPayrollProcessor extends Actor {

  def receive = {
	  case SinglePayroll(employeeId, baseSalary, daysOff, perks) => 
	    val totalSalary = (baseSalary + perks) - (baseSalary * 0.02) * daysOff
	    //TODO: save the result to some storage
	
			sender ! WorkerDone	     
  }	
}

class MasterPayrollProcessor(nrOfWorkers: Int) extends Actor {
	
	val workerRouter = context.actorOf(
    Props[WorkerPayrollProcessor].withRouter(RoundRobinRouter(nrOfWorkers)))

  val parseLine = """(\w+),(\d+),(\d+),(\d+)""".r
  var noOfResults: Int = _
  var noOfMessages: Int = _  
  var initiator: ActorRef = _
	
	def receive = {
		case PayrollChunk(payload) => 
		 noOfMessages += payload.size
		 initiator = sender 
		 payload.foreach { line =>
			  val parseLine(employeeId, baseSalary, daysOff, perks) = line
			  workerRouter ! SinglePayroll(employeeId, BigDecimal(baseSalary), daysOff.toInt, BigDecimal(perks))
		  }
		case WorkerDone =>
		  noOfResults += 1
		  if(noOfMessages == noOfResults) {
			  initiator ! MasterDone
			}
	}
}

class PayrollProcessor(processors: List[ActorRef]) extends Actor {
	def receive = {
		case PayrollFile(fileName) => 
		  processPayroll(fileName)
		case MasterDone => 
		  println("Payload is processed by " + sender)
	}
	
	def processPayroll(fileName: String) {
	  val router = context.actorOf(Props[MasterPayrollProcessor].withRouter(SmallestMailboxRouter(processors)))
	  val payloads = Source.fromFile(fileName).getLines.grouped(100).toList
		payloads.foreach(payload => router ! PayrollChunk(payload))
	}
}

object Main extends App {
	val system = ActorSystem("PayrollSystem", ConfigFactory.load.getConfig("payroll"))
	// create the master
  val master1 = system.actorOf(Props(new MasterPayrollProcessor(10)), name = "master1")
  val master2 = system.actorOf(Props(new MasterPayrollProcessor(10)), name = "master2")

	val processor = system.actorOf(Props(new PayrollProcessor(List(master1, master2))), name = "processor")
	processor ! PayrollFile("src/main/resources/payroll.txt") 
	Thread.sleep(2000)
	system.shutdown()
}


