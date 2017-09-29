package utils.testdata

trait ArchiveableJsonGenerators { self: BaseDummyData =>

  def addArchiveJsonStr(
      title: String,
      desc: Option[String]
  ) = {
    s"""{
       |  "title" : "$title",
       |  ${desc.map(d => s""""description" : "$d",""").getOrElse("")}
       |  "published" : false,
       |  "documentMedium" : "digital",
       |  "type" : "archive"
       |}
     """.stripMargin
  }

  def addArchivePartJsonStr(
      title: String,
      desc: Option[String]
  ) = {
    s"""{
       |  "title" : "$title",
       |  ${desc.map(d => s""""description" : "$d",""").getOrElse("")}
       |  "published" : false,
       |  "documentMedium" : "digital",
       |  "type" : "archive_part"
       |}
     """.stripMargin
  }

  def addArchiveFolderJsonStr(
      title: String,
      desc: Option[String]
  ) = {
    s"""{
       |  "title" : "$title",
       |  ${desc.map(d => s""""description" : "$d",""").getOrElse("")}
       |  "published" : false,
       |  "documentMedium" : "digital",
       |  "type" : "archive_folder"
       |}
     """.stripMargin
  }

}
