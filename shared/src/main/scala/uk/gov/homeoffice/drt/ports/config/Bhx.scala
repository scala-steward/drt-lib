package uk.gov.homeoffice.drt.ports.config

import uk.gov.homeoffice.drt.auth.Roles.BHX
import uk.gov.homeoffice.drt.ports.PaxTypes._
import uk.gov.homeoffice.drt.ports.PaxTypesAndQueues._
import uk.gov.homeoffice.drt.ports.Queues._
import uk.gov.homeoffice.drt.ports.SplitRatiosNs.{SplitRatio, SplitRatios, SplitSources}
import uk.gov.homeoffice.drt.ports.Terminals._
import uk.gov.homeoffice.drt.ports._
import uk.gov.homeoffice.drt.time.LocalDate

import scala.collection.immutable.SortedMap

object Bhx extends AirportConfigLike {

  import AirportConfigDefaults._

  private object ProcTimes {
    val gbr = 24.0
    val eea = 37.0
    val b5jssk = 52.0
    val nvn = 87.0
    val vn = 88.0
    val egates = 48d
  }

  val config: AirportConfig = AirportConfig(
    portCode = PortCode("BHX"),
    portName = "Birmingham",
    queuesByTerminal = SortedMap(
      LocalDate(2014, 1, 1) -> SortedMap(
        T1 -> Seq(EeaDesk, EGate, NonEeaDesk),
        T2 -> Seq(EeaDesk, NonEeaDesk)
      ),
      LocalDate(2025, 8, 4) -> SortedMap(
        T1 -> Seq(EeaDesk, EGate, NonEeaDesk),
        T2 -> Seq(QueueDesk)
      ),
    ),
    slaByQueue = defaultSlas ++ Map(
      QueueDesk -> 60,
    ),
    defaultWalkTimeMillis = Map(T1 -> 240000L, T2 -> 240000L),
    terminalPaxSplits = Map(
      T1 -> SplitRatios(
        SplitSources.TerminalAverage,
        SplitRatio(eeaMachineReadableToDesk, 0.92 * 0.2446),
        SplitRatio(eeaMachineReadableToEGate, 0.92 * 0.7554),
        SplitRatio(eeaNonMachineReadableToDesk, 0),
        SplitRatio(visaNationalToDesk, 0.04),
        SplitRatio(nonVisaNationalToDesk, 0.04)
      ),
      T2 -> SplitRatios(
        SplitSources.TerminalAverage,
        SplitRatio(eeaMachineReadableToDesk, 0.92),
        SplitRatio(eeaNonMachineReadableToDesk, 0),
        SplitRatio(visaNationalToDesk, 0.04),
        SplitRatio(nonVisaNationalToDesk, 0.04)
      )),
    terminalProcessingTimes = Map(T1 -> Map(
      b5jsskToDesk -> ProcTimes.b5jssk / 60,
      b5jsskChildToDesk -> ProcTimes.b5jssk / 60,
      eeaMachineReadableToDesk -> ProcTimes.eea / 60,
      eeaNonMachineReadableToDesk -> ProcTimes.eea / 60,
      eeaChildToDesk -> ProcTimes.eea / 60,
      gbrNationalToDesk -> ProcTimes.gbr / 60,
      gbrNationalChildToDesk -> ProcTimes.gbr / 60,
      b5jsskToEGate -> ProcTimes.egates / 60,
      eeaMachineReadableToEGate -> ProcTimes.egates / 60,
      gbrNationalToEgate -> ProcTimes.egates / 60,
      visaNationalToDesk -> ProcTimes.vn / 60,
      nonVisaNationalToDesk -> ProcTimes.nvn / 60
    ), T2 -> Map(
      b5jsskToDesk -> ProcTimes.b5jssk / 60,
      b5jsskChildToDesk -> ProcTimes.b5jssk / 60,
      eeaMachineReadableToDesk -> ProcTimes.eea / 60,
      eeaNonMachineReadableToDesk -> ProcTimes.eea / 60,
      eeaChildToDesk -> ProcTimes.eea / 60,
      gbrNationalToDesk -> ProcTimes.gbr / 60,
      gbrNationalChildToDesk -> ProcTimes.gbr / 60,
      b5jsskToEGate -> ProcTimes.egates / 60,
      eeaMachineReadableToEGate -> ProcTimes.egates / 60,
      gbrNationalToEgate -> ProcTimes.egates / 60,
      visaNationalToDesk -> ProcTimes.vn / 60,
      nonVisaNationalToDesk -> ProcTimes.nvn / 60
    )),
    minMaxDesksByTerminalQueue24Hrs = Map(
      T1 -> Map(
        EGate -> (List(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1),
          List(2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2)),
        EeaDesk -> (List(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1),
          List(6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6)),
        NonEeaDesk -> (List(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1),
          List(8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8))
      ),
      T2 -> Map(
        EeaDesk -> (List(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1),
          List(4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4)),
        NonEeaDesk -> (List(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1),
          List(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1)),
        QueueDesk -> (List(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1),
          List(8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8)),
      )
    ),
    eGateBankSizes = Map(T1 -> Iterable(10, 5)),
    hasEstChox = false,
    role = BHX,
    terminalPaxTypeQueueAllocation = Map(
      T1 -> (
        defaultQueueRatios +
          (EeaMachineReadable -> List(EGate -> 0.7968, EeaDesk -> (1.0 - 0.7968))),
        ),
      T2 -> defaultQueueRatiosWithoutEgates
    ),
    feedSources = Seq(ApiFeedSource, LiveBaseFeedSource, LiveFeedSource, ForecastFeedSource, AclFeedSource),
    flexedQueues = Set(EeaDesk, NonEeaDesk),
    desksByTerminal = Map[Terminal, Int](T1 -> 9, T2 -> 8)
  )
}
