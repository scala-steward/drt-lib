package drt.auth

object Roles {
  val portRoles: Set[Role] = Set(
    BHXAccess,
    BRSAccess,
    EDIAccess,
    EMAAccess,
    LGWAccess,
    GLAAccess,
    LCYAccess,
    BFSAccess,
    LPLAccess,
    NCLAccess,
    LHRAccess,
    LTNAccess,
    MANAccess,
    TestAccess,
    Test2Access,
    STNAccess
  )
  val availableRoles: Set[Role] = Set(
    FixedPointsEdit,
    StaffMovementsEdit,
    StaffMovementsExport,
    StaffEdit,
    ApiView,
    ManageUsers,
    CreateAlerts,
    ApiViewPortCsv,
    FixedPointsEdit,
    FixedPointsView,
    DesksAndQueuesView,
    ArrivalsAndSplitsView,
    ForecastView,
    BorderForceStaff,
    PortOperatorStaff,
    PortFeedUpload,
    ViewConfig,
    TerminalDashboard,
    ArrivalSource,
    ArrivalSimulationUpload,
    EnhancedApiView,
    Debug,
    FaqView
  ) ++ portRoles

  def parse(roleName: String): Option[Role] = availableRoles.find(role => role.name == roleName)
}
