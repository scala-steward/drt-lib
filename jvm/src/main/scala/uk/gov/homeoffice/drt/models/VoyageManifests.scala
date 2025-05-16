package uk.gov.homeoffice.drt.models

case class VoyageManifests(manifests: Iterable[VoyageManifest]) {

  def toMap: Map[ManifestKey, VoyageManifest] = manifests.collect {
    case vm if vm.maybeKey.isDefined =>
      vm.maybeKey.get -> vm
  }.toMap

  def ++(other: VoyageManifests): VoyageManifests = VoyageManifests(manifests ++ other.manifests)
}

object VoyageManifests {
  def empty: VoyageManifests = VoyageManifests(Set())
}
