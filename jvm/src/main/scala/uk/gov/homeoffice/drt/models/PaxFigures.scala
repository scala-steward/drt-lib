package uk.gov.homeoffice.drt.models

import uk.gov.homeoffice.drt.ports.Terminals.Terminal
import uk.gov.homeoffice.drt.time.LocalDate


case class PaxFigures(terminal: Terminal, models: Seq[String], paxFigures: Seq[DayPaxFigures])

case class DayPaxFigures(date: LocalDate, actPax: Int, actCapPct: Double, forecastPax: Int, forecastCapPct: Double, drtFcstPax: Int, drtFcstCapPct: Double, modelPax: Map[String, Int], modelCapPct: Map[String, Double])
