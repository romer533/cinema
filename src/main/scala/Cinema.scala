
import CinemaRoomEvent.{AddedItem, Booked, Unbooked, applyEvent}
import akka.actor.{ActorSystem, Props}
import akka.persistence._

case class BuyPlace(placeId: Int)

case class ReturnPlace(placeId: Int)

case class BuyPillow(placeId: Int)

case class BuyPlaid(placeId: Int)

case class BuyMattress(placeId: Int)

sealed trait BookState

case class IsFree() extends BookState

case class IsBooked(items: List[String] = Nil) extends BookState

sealed trait CinemaRoomEvent

object CinemaRoomEvent {

  case class Booked(placeId: Int) extends CinemaRoomEvent

  case class Unbooked(placeId: Int) extends CinemaRoomEvent

  case class AddedItem(placeId: Int, item: String) extends CinemaRoomEvent

  def applyEvent(currentState: State, event: CinemaRoomEvent): State = event match {
    case Booked(placeId) => State(currentState.places.patch(placeId, List(IsBooked()), 1))
    case Unbooked(placeId) => State(currentState.places.patch(placeId, List(IsFree()), 1)) //State(currentState.places + (placeId -> Map(value -> false)))
    case AddedItem(placeId, item) => State(currentState.places.map {
      case IsFree() => IsFree()
      case IsBooked(items) => IsBooked(items.patch(placeId, List(item), 0))
    })
  }

}

case class State(places: List[BookState]) {

  def update(event: CinemaRoomEvent): State = event match {
    case booked: Booked => applyEvent(copy(places), booked)
    case unbooked: Unbooked => applyEvent(copy(places), unbooked)
    case addedItem: AddedItem => applyEvent(copy(places), addedItem)
  }

}

class CinemaActor(defaultState: State) extends PersistentActor {
  override def persistenceId: String = "cinema-id-3"

  var state: State = defaultState

  def updateState(event: CinemaRoomEvent): Unit =
    state = state.update(event)

  override def receiveRecover: Receive = {
    case evt: CinemaRoomEvent => updateState(evt)
    case SnapshotOffer(_, snapshot: State) => state = snapshot
  }

  val snapshotInterval = 100

  override def receiveCommand: Receive = {
    case BuyPlace(placeId) if state.places(placeId) == IsFree() =>
      println(s"Place $placeId is book")
      persist(Booked(placeId)) { event =>
        updateState(event)
        context.system.eventStream.publish(event)
        if (lastSequenceNr % snapshotInterval == 0 && lastSequenceNr != 0) saveSnapshot(state)
      }
    case ReturnPlace(placeId) =>
      println(s"Place $placeId is free")
      persist(Unbooked(placeId)) { event =>
        updateState(event)
        context.system.eventStream.publish(event)
        if (lastSequenceNr % snapshotInterval == 0 && lastSequenceNr != 0) saveSnapshot(state)
      }
    case BuyPillow(placeId) if {
      state.places(placeId) != IsFree() &&
      state.places(placeId) != IsBooked(List("pillow"))
    } =>
      println(s"Buy pillow for $placeId place")
      persist(AddedItem(placeId, "pillow")) { event =>
        updateState(event)
        context.system.eventStream.publish(event)
        if (lastSequenceNr % snapshotInterval == 0 && lastSequenceNr != 0) saveSnapshot(state)
      }
    case BuyPlaid(placeId) if {
      state.places(placeId) != IsFree() &&
      state.places(placeId) != IsBooked(List("plaid"))
    } =>
      println(s"Buy plaid for $placeId place")
      persist(AddedItem(placeId, "plaid")) { event =>
        updateState(event)
        context.system.eventStream.publish(event)
        if (lastSequenceNr % snapshotInterval == 0 && lastSequenceNr != 0) saveSnapshot(state)
      }
    case BuyMattress(placeId) if
      state.places(placeId) == IsBooked(List("pillow", "plaid")) ||
      state.places(placeId) == IsBooked(List("plaid", "pillow")) =>
      println(s"Buy mattress for $placeId place")
      persist(AddedItem(placeId, "mattress")) { event =>
        updateState(event)
        context.system.eventStream.publish(event)
        if (lastSequenceNr % snapshotInterval == 0 && lastSequenceNr != 0) saveSnapshot(state)
      }
    case _ => println(s"Some message || ${state.places(12)}")
  }
}

object Cinema extends App {

  val system = ActorSystem("cinema")
  val size = 100
  val defaultState = State(List.fill(size)(IsFree()))
  val cinemaActor = system.actorOf(Props(new CinemaActor(defaultState)), name = "cinema-1-zone")

  cinemaActor ! BuyPlace(2)

  Thread.sleep(1000)
  system.terminate()

}
