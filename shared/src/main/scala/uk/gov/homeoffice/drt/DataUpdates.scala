package uk.gov.homeoffice.drt

object DataUpdates {

  trait Combinable[A] {
    def ++(other: A): A
  }

  trait Updates

  trait FlightUpdates extends Updates

  trait MinuteUpdates extends Updates

}
