package uk.gov.homeoffice.drt.actor.serialisation

import org.scalatest.wordspec.AnyWordSpec
import uk.gov.homeoffice.drt.actor.ConfigActor.SetUpdate
import uk.gov.homeoffice.drt.ports.Queues
import uk.gov.homeoffice.drt.ports.Queues.{EGate, EeaDesk, NonEeaDesk}
import uk.gov.homeoffice.drt.ports.config.slas.{SlaConfigs, SlasUpdate}

import scala.collection.immutable.SortedMap

class SlaConfigsSerialisationSpec extends AnyWordSpec {
  val serialiser: ConfigSerialiser[Map[Queues.Queue, Int], SlaConfigs] = ConfigSerialiser.slaConfigsSerialiser
  val deserialiser: ConfigDeserialiser[Map[Queues.Queue, Int], SlaConfigs] = ConfigDeserialiser.slaConfigsDeserialiser

  "SlaConfigs" should {
    "serialise and deserialise without loss" in {
      val slaConfigs = SlaConfigs(SortedMap(
        1L -> Map(EeaDesk -> 1, NonEeaDesk -> 10),
        2L -> Map(EeaDesk -> 2, EGate -> 5),
      ))
      val serialised = serialiser.updatesWithHistory(slaConfigs)
      val deserialised = deserialiser.deserialiseState(serialised)
      assert(deserialised == slaConfigs)
    }
  }

  "SetUpdate" should {
    "serialise and deserialise without loss" in {
      val setUpdate = SetUpdate(SlasUpdate(
        effectiveFrom = 1L,
        configItem = Map(EeaDesk -> 1, NonEeaDesk -> 10),
        maybeOriginalEffectiveFrom = Option(2L),
      ))
      val serialised = serialiser.setUpdate(setUpdate)
      val deserialised = deserialiser.deserialiseCommand(serialised)
      assert(deserialised == setUpdate)
    }
  }
}
