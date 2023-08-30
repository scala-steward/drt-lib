package uk.gov.homeoffice.drt.ports

sealed trait PortRegion {
  val name: String
  val ports: Set[PortCode]
}

object PortRegion {
  val regions: Set[PortRegion] = Set(North, South, Central, Heathrow)

  val ports: Set[PortCode] = regions.flatMap(_.ports)

  object North extends PortRegion {
    override val name: String = "North"
    override val ports: Set[PortCode] =
      Set("ABZ", "BFS", "BHD", "EDI", "GLA", "HUY", "INV", "LBA", "LPL", "MAN", "MME", "NCL", "PIK").map(PortCode(_))
  }

  object Central extends PortRegion {
    override val name: String = "Central"
    override val ports: Set[PortCode] = Set("BHX", "EMA", "LCY", "LTN", "NWI", "SEN", "STN").map(PortCode(_))
  }

  object South extends PortRegion {
    override val name: String = "South"
    override val ports: Set[PortCode] = Set("BOH", "BRS", "CWL", "EXT", "LGW", "NQY", "SOU").map(PortCode(_))
  }

  object Heathrow extends PortRegion {
    override val name: String = "Heathrow"
    override val ports: Set[PortCode] = Set(PortCode("LHR"))
  }

  def fromPort(portCode: PortCode): PortRegion = regions.find(_.ports.contains(portCode)).getOrElse(throw new Exception(s"Unknown port $portCode"))
}
