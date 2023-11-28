package uk.gov.homeoffice.drt.actor

trait PersistentDrtActor[T] {

  def state: T

  def initialState: T
}
