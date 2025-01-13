package uk.gov.homeoffice.drt.keycloak

case class KeyCloakUser(id: String, username: String, enabled: Boolean, emailVerified: Boolean, firstName: String, lastName: String, email: String)

case class KeyCloakGroup(id: String, name: String, path: String)

