
import CinemaRoomEvent.{AddedItem, Booked, Unbooked, applyEvent}
import akka.actor.{ActorSystem, Props}
import akka.persistence._

case class BuyPlace(placeId: Int)
case class BuyPillow(placeId: Int)
case class BuyPlaid(placeId: Int)
case class BuyMattress(placeId: Int)


sealed trait CinemaRoomEvent
object CinemaRoomEvent {

  case class Booked(placeId: Int) extends CinemaRoomEvent
  case class Unbooked(placeId: Int) extends CinemaRoomEvent
  case class AddedItem(placeId: Int, item:  String) extends CinemaRoomEvent

  def applyEvent(currentState: State, event: CinemaRoomEvent): State = event match {
    case Booked(placeId) => State(currentState.places + (placeId -> Map("placeId" -> true)))
    case Unbooked(placeId) => State(currentState.places + (placeId -> Map("placeId" -> false)))
    case AddedItem(placeId, item) => State(currentState.places + (placeId -> Map(item -> true)))
  }

}

case class State(places: Map[Int, Map[String, Boolean]] = Map(0 -> Map("placeId" -> false))) {

  def update(event: CinemaRoomEvent): State = event match {
    case booked: Booked => applyEvent(copy(places), booked)
    case unbooked: Unbooked => applyEvent(copy(places), unbooked)
    case addedItem: AddedItem => applyEvent(copy(places), addedItem)
  }

}

class CinemaActor extends PersistentActor {
  override def persistenceId: String = "cinema-id-1"

  var state: State = State()

  def updateState(event: CinemaRoomEvent): Unit =
    state = state.update(event)

  override def receiveRecover: Receive = {
    case evt: CinemaRoomEvent => updateState(evt)
    case SnapshotOffer(_, snapshot: State) => state = snapshot
  }

  val snapshotInterval = 100

  override def receiveCommand: Receive = {
    case BuyPlace(placeId) =>
      println(s"Place $placeId is booked")
      persist(Booked(placeId)) { event =>
        updateState(event)
        context.system.eventStream.publish(event)
        if (lastSequenceNr % snapshotInterval == 0 && lastSequenceNr != 0) saveSnapshot(state)
      }
    case BuyPillow(placeId) =>
      println(s"Buy pillow for $placeId place")
      persist(AddedItem(placeId, "pillow")) { event =>
        updateState(event)
        context.system.eventStream.publish(event)
        if (lastSequenceNr % snapshotInterval == 0 && lastSequenceNr != 0) saveSnapshot(state)
      }
    case BuyPlaid(placeId) =>
      println(s"Buy plaid for $placeId place")
      persist(AddedItem(placeId, "plaid")) { event =>
        updateState(event)
        context.system.eventStream.publish(event)
        if (lastSequenceNr % snapshotInterval == 0 && lastSequenceNr != 0) saveSnapshot(state)
      }
    case BuyMattress(placeId) =>
      println(s"Buy mattress for $placeId place")
      persist(AddedItem(placeId, "mattress")) { event =>
        updateState(event)
        context.system.eventStream.publish(event)
      }
  }
}

object Cinema extends App {

  val system = ActorSystem("cinema")
  val cinemaActor = system.actorOf(Props[CinemaActor](), "cinema-1-zone")

  cinemaActor ! BuyPlace(0)
  cinemaActor ! BuyPlace(1)
  cinemaActor ! BuyPlace(3)
  cinemaActor ! BuyPlace(2)
  cinemaActor ! BuyPlace(5)

  cinemaActor ! BuyPlaid(3)
  cinemaActor ! BuyPlaid(1)
  cinemaActor ! BuyPlaid(0)

  cinemaActor ! BuyPillow(5)
  cinemaActor ! BuyPillow(2)

  Thread.sleep(5000)
  system.terminate()

}
