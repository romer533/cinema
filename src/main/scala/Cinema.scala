
import CinemaRoomEvent.{AddedItem, Booked, Unbooked, applyEvent}
import akka.actor.{ActorSystem, Props}
import akka.persistence._

case class BuyPlace(placeId: Int)
case class ReturnPlace(placeId: Int)
case class BuyPillow(placeId: Int)
case class BuyPlaid(placeId: Int)
case class BuyMattress(placeId: Int)

sealed trait BookState
case class Free() extends BookState
case class Booked(item: List[String] = List("Pillow", "Plaid", "Mattress")) extends BookState

sealed trait CinemaRoomEvent
object CinemaRoomEvent {

  case class Booked(placeId: Int, value: Int) extends CinemaRoomEvent
  case class Unbooked(placeId: Int, value: Int) extends CinemaRoomEvent
  case class AddedItem(placeId: Int, item:  Int) extends CinemaRoomEvent

  def applyEvent(currentState: State, event: CinemaRoomEvent): State = event match {
    case Booked(placeId, value) => State(currentState.places + (placeId -> Map(value -> true)))
    case Unbooked(placeId, value) => State(currentState.places + (placeId -> Map(value -> false)))
    case AddedItem(placeId, item) => State(currentState.places + (placeId -> Map(item -> true)))
  }

}

case class State(places: Map[Int, Map[Int, Boolean]] = Map(0 -> Map(0 -> false))) {

  def update(event: CinemaRoomEvent): State = event match {
    case booked: Booked => applyEvent(copy(places), booked)
    case unbooked: Unbooked => applyEvent(copy(places), unbooked)
    case addedItem: AddedItem => applyEvent(copy(places), addedItem)
  }

}

class CinemaActor extends PersistentActor {
  override def persistenceId: String = "cinema-id-2"

  var state: State = State()

  def updateState(event: CinemaRoomEvent): Unit =
    state = state.update(event)

  override def receiveRecover: Receive = {
    case evt: CinemaRoomEvent => updateState(evt)
    case SnapshotOffer(_, snapshot: State) => state = snapshot
  }

  val snapshotInterval = 100

  override def receiveCommand: Receive = {
    case BuyPlace(placeId) if (state.places get placeId) == Some(Map(0 -> false)) =>
      println(s"Place $placeId is book")
      persist(Booked(placeId, 0)) { event =>
        updateState(event)
        context.system.eventStream.publish(event)
        if (lastSequenceNr % snapshotInterval == 0 && lastSequenceNr != 0) saveSnapshot(state)
      }
    case ReturnPlace(placeId) if state.places.contains(placeId) =>
      println(s"Place $placeId is free")
      persist(Unbooked(placeId, 0)) { event =>
        updateState(event)
        context.system.eventStream.publish(event)
        if (lastSequenceNr % snapshotInterval == 0 && lastSequenceNr != 0) saveSnapshot(state)
      }
    case BuyPillow(placeId) =>
      println(s"Buy pillow for $placeId place")
      persist(AddedItem(placeId, 1)) { event =>
        updateState(event)
        context.system.eventStream.publish(event)
        if (lastSequenceNr % snapshotInterval == 0 && lastSequenceNr != 0) saveSnapshot(state)
      }
    case BuyPlaid(placeId) =>
      println(s"Buy plaid for $placeId place")
      persist(AddedItem(placeId, 2)) { event =>
        updateState(event)
        context.system.eventStream.publish(event)
        if (lastSequenceNr % snapshotInterval == 0 && lastSequenceNr != 0) saveSnapshot(state)
      }
    case BuyMattress(placeId) =>
      println(s"Buy mattress for $placeId place")
      persist(AddedItem(placeId, 3)) { event =>
        updateState(event)
        context.system.eventStream.publish(event)
      }
    case _ => println("Some message")
  }
}

object Cinema extends App {

  val system = ActorSystem("cinema")
  val cinemaActor = system.actorOf(Props[CinemaActor](), "cinema-1-zone")

  cinemaActor ! BuyPlace(1)
  cinemaActor ! BuyPlace(2)
  cinemaActor ! BuyPlace(3)
  cinemaActor ! BuyPlace(4)
  cinemaActor ! ReturnPlace(1)
  cinemaActor ! BuyPlace(1)

  Thread.sleep(5000)
  system.terminate()

}
