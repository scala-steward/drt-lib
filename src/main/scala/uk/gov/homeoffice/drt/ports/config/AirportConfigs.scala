package uk.gov.homeoffice.drt.ports.config

import uk.gov.homeoffice.drt.ports.{AirportConfig, AirportConfigLike, PortCode}

object AirportConfigs {
  val allPorts: List[AirportConfigLike] = List(Bfs, Bhd, Bhx, Brs, Edi, Ema, Gla, Lba, Lcy, Lgw, Lhr, Lpl, Ltn, Man, Ncl, Pik, Stn)

  val allPortConfigs: List[AirportConfig] = allPorts.map(_.config)

  def portGroups: List[String] = allPortConfigs.map(_.portCode.toString.toUpperCase).sorted

  val confByPort: Map[PortCode, AirportConfig] = allPortConfigs.map(c => (c.portCode, c)).toMap
}
