package uk.gov.homeoffice.drt.egates

import org.scalatest.wordspec.AnyWordSpec
import uk.gov.homeoffice.drt.ports.Terminals.T1

class PortEgateBanksUpdatesTest extends AnyWordSpec {
  "A empty PortEgateBanksUpdates" should {
    "Reflect an update" in {
      val portEgateBanksUpdates = PortEgateBanksUpdates(Map())
      val update = EgateBanksUpdate(150L, IndexedSeq(EgateBank(IndexedSeq(true, false))))
      val updated = portEgateBanksUpdates.update(SetEgateBanksUpdate(T1, 100L, update))

      updated === PortEgateBanksUpdates(Map(T1 -> EgateBanksUpdates(List(update))))
    }
  }

  "A non-empty PortEgateBanksUpdates" should {
    "Replace an existing update when the original date matches the existing update's effective from date" in {
      val originalUpdate = EgateBanksUpdate(150L, IndexedSeq(EgateBank(IndexedSeq(true, false))))
      val replacementUpdate = EgateBanksUpdate(200L, IndexedSeq(EgateBank(IndexedSeq(false, false))))
      val portEgateBanksUpdates = PortEgateBanksUpdates(Map(T1 -> EgateBanksUpdates(List(originalUpdate))))
      val updated = portEgateBanksUpdates.update(SetEgateBanksUpdate(T1, 150L, replacementUpdate))

      updated === PortEgateBanksUpdates(Map(T1 -> EgateBanksUpdates(List(replacementUpdate))))
    }
  }

  "A non-empty PortEgateBanksUpdates" should {
    "Add to an existing update when the original date doesn't match the existing update's effective from date" in {
      val originalUpdate = EgateBanksUpdate(150L, IndexedSeq(EgateBank(IndexedSeq(true, false))))
      val replacementUpdate = EgateBanksUpdate(200L, IndexedSeq(EgateBank(IndexedSeq(false, false))))
      val portEgateBanksUpdates = PortEgateBanksUpdates(Map(T1 -> EgateBanksUpdates(List(originalUpdate))))
      val updated = portEgateBanksUpdates.update(SetEgateBanksUpdate(T1, 200L, replacementUpdate))

      updated === PortEgateBanksUpdates(Map(T1 -> EgateBanksUpdates(List(originalUpdate, replacementUpdate))))
    }
  }

  "A non-empty PortEgateBanksUpdates" should {
    "Remove an update with a matching effective from date" in {
      val originalUpdate = EgateBanksUpdate(150L, IndexedSeq(EgateBank(IndexedSeq(true, false))))
      val remove = DeleteEgateBanksUpdates(T1, 150L)
      val portEgateBanksUpdates = PortEgateBanksUpdates(Map(T1 -> EgateBanksUpdates(List(originalUpdate))))
      val updated = portEgateBanksUpdates.remove(remove)

      updated === PortEgateBanksUpdates(Map(T1 -> EgateBanksUpdates(List())))
    }
  }

  "A non-empty PortEgateBanksUpdates" should {
    "Remain the same given a deletion for a time that doesn't exist" in {
      val originalUpdate = EgateBanksUpdate(150L, IndexedSeq(EgateBank(IndexedSeq(true, false))))
      val remove = DeleteEgateBanksUpdates(T1, 200L)
      val portEgateBanksUpdates = PortEgateBanksUpdates(Map(T1 -> EgateBanksUpdates(List(originalUpdate))))
      val updated = portEgateBanksUpdates.remove(remove)

      updated === PortEgateBanksUpdates(Map(T1 -> EgateBanksUpdates(List(originalUpdate))))
    }
  }
}
