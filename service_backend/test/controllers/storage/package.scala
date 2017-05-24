package controllers

package object storage {

  val BaseUrl            = "/museum"
  val StorageNodesUrl    = (mid: Int) => s"$BaseUrl/$mid/storagenodes"
  val RootNodeUrl        = (mid: Int) => s"$BaseUrl/$mid/storagenodes/root"
  val StorageNodeUrl     = (mid: Int, node: String) => s"$BaseUrl/$mid/storagenodes/$node"
  val MoveStorageNodeUrl = (mid: Int) => s"${StorageNodesUrl(mid)}/moveNode"
  val MoveObjectUrl      = (mid: Int) => s"${StorageNodesUrl(mid)}/moveObject"

  val NodeChildrenUrl = (mid: Int, node: String) =>
    s"${StorageNodeUrl(mid, node)}/children"

  val ObjLocationHistoryUrl = (mid: Int, objectId: String) =>
    s"${StorageNodesUrl(mid)}/objects/$objectId/locations"

  val ControlsUrl = (mid: Int, node: String) => s"${StorageNodeUrl(mid, node)}/controls"
  val ControlUrl = (mid: Int, node: String, evt: Long) =>
    s"${ControlsUrl(mid, node)}/$evt"

  val ObservationsUrl = (mid: Int, node: String) =>
    s"${StorageNodeUrl(mid, node)}/observations"

  val ObservationUrl = (mid: Int, node: String, evt: Long) =>
    s"${ObservationsUrl(mid, node)}/$evt"

  val CtrlObsForNodeUrl = (mid: Int, node: String) =>
    s"${StorageNodeUrl(mid, node)}/events"

  val KdReportUrl = (mid: Int) => s"$BaseUrl/$mid/storagenodes/report"

  val ObjCurrentLocationUrl = (mid: Int, objectId: String) =>
    s"${StorageNodesUrl(mid)}/objects/$objectId/currentlocation"

  val StorageNodeSearchName = (mid: Int) => s"${StorageNodesUrl(mid)}/search"

  val ScanUrl = (mid: Int) => s"${StorageNodesUrl(mid)}/scan"

  val HundredAndOneCharString =
    """abcdefghijklmnopqrstuvwxyzæøåa
      |abcdefghijklmnopqrstuvwxyzæøåa
      |abcdefghijklmnopqrstuvwxyzæøåa
      |abcdefghijk""".stripMargin.replaceAll("\n", "")

  val VeryLongString =
    """12345678901234567890123456789012345678901234567890
      |12345678901234567890123456789012345678901234567890
      |12345678901234567890123456789012345678901234567890
      |12345678901234567890123456789012345678901234567890
      |12345678901234567890123456789012345678901234567890
      |12345678901234567890123456789012345678901234567890
      |12345678901234567890123456789012345678901234567890
      |12345678901234567890123456789012345678901234567890
      |12345678901234567890123456789012345678901234567890
      |12345678901234567890123456789012345678901234567890
      |12345678901234567890123456789012345678901234567890
      |12345678901234567890123456789012345678901234567890
      |""".stripMargin.replaceAll("\n", " ")

}
