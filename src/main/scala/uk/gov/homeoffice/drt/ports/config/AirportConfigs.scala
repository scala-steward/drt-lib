package uk.gov.homeoffice.drt.ports.config

import uk.gov.homeoffice.drt.ports.{AirportConfig, AirportConfigLike, PortCode}

object AirportConfigs {
  val allPorts: List[AirportConfigLike] = List(Abz, Bfs, Bhd, Bhx, Brs, Boh, Cwl, Dsa, Edi, Ema, Ext, Gla, Huy, Inv, Lba, Lcy, Lgw, Lhr, Lpl, Ltn, Man, Mme, Ncl, Nqy, Nwi, Pik, Sen, Sou, Stn)

  val allPortConfigs: List[AirportConfig] = allPorts.map(_.config)

  def portGroups: List[String] = allPortConfigs.map(_.portCode.toString.toUpperCase).sorted

  val confByPort: Map[PortCode, AirportConfig] = allPortConfigs.map(c => (c.portCode, c)).toMap
}
