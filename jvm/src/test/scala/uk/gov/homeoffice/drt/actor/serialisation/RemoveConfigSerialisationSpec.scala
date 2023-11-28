package uk.gov.homeoffice.drt.actor.serialisation

import org.scalatest.wordspec.AnyWordSpec
import scalapb.GeneratedMessage
import uk.gov.homeoffice.drt.actor.ConfigActor
import uk.gov.homeoffice.drt.actor.ConfigActor.{RemoveConfig, SetUpdate}
import uk.gov.homeoffice.drt.ports.Queues.Queue
import uk.gov.homeoffice.drt.ports.config.slas.SlaConfigs

class RemoveConfigSerialisationSpec extends AnyWordSpec {
  val serialiser = new ConfigSerialiser[Map[Queue, Int], SlaConfigs] {
    override def updatesWithHistory(a: SlaConfigs): GeneratedMessage = ???

    override def setUpdate(a: SetUpdate[Map[Queue, Int]], createdAt: Long): GeneratedMessage = ???
  }

  val deserialiser = new ConfigDeserialiser[Map[Queue, Int], SlaConfigs] {
    override def deserialiseCommand(a: GeneratedMessage): ConfigActor.Command = ???

    override def deserialiseState(a: GeneratedMessage): SlaConfigs = ???
  }


  "RemoveUpdate" should {
    "serialise and deserialise without loss" in {
      val removeUpdate = RemoveConfig(1L)
      val serialised = serialiser.removeUpdate(removeUpdate, 1L)
      val deserialised = deserialiser.removeUpdate(serialised)
      assert(deserialised == removeUpdate)
    }
  }
}
