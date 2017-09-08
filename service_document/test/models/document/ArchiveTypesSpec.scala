package models.document

import models.document.ArchiveTypes.{Archive, ArchiveFolder, ArchivePart}
import models.document.ArchiveIdentifiers._
import models.document.Archiveables.ArchiveFolderItem
import net.scalytica.symbiotic.api.types.CustomMetadataAttributes.Implicits._
import net.scalytica.symbiotic.api.types.PersistentType.UserStamp
import net.scalytica.symbiotic.api.types.ResourceParties.{Owner, Usr}
import net.scalytica.symbiotic.api.types.{FileId, Folder, Lock, Path}
import no.uio.musit.models.MuseumCollections
import org.joda.time.DateTime
import org.scalatest.{MustMatchers, OptionValues, WordSpec}

class ArchiveTypesSpec extends WordSpec with MustMatchers with OptionValues {

  val archUser  = ArchiveUserId.create()
  val timestamp = DateTime.now().minusDays(7)

  type FolderItemInit[A] = (
      Option[ArchiveId],
      Option[FileId],
      String,
      Option[String],
      Option[Owner],
      Option[ArchiveCollectionId],
      Option[Path],
      Option[Lock],
      Boolean,
      Option[String],
      Option[UserStamp],
      Option[UserStamp]
  ) => A

  // scalastyle:off
  def createArchiveFolderItem[A <: ArchiveFolderItem](
      fid: Option[FileId],
      title: String,
      description: Option[String]
  )(init: FolderItemInit[A]) = init(
    ArchiveId.generateAsOpt(),
    fid,
    title,
    description,
    Some(Owner(archUser, Usr)),
    Some(MuseumCollections.Archeology.uuid),
    Some(Path("/foo/bar/baz")),
    Some(Lock(archUser, timestamp.plusDays(2))),
    false,
    Some("digital"),
    Some(
      UserStamp(
        by = archUser,
        date = timestamp.plusDays(6)
      )
    ),
    Some(
      UserStamp(
        by = archUser,
        date = timestamp
      )
    )
  )

  // scalastyle:on

  "An Archive" should {

    val expected = createArchiveFolderItem(
      fid = FileId.createOpt(),
      title = "test archive",
      description = Some("this is a test")
    )(Archive.apply)

    "be implicitly converted to a symbiotic Folder" in {
      val actual: Folder = expected

      actual.id.map(_.toString) mustBe expected.id.map(_.asString)
      actual.filename mustBe expected.title
      actual.fileType mustBe Some(Archive.FolderType)
      actual.uploadDate mustBe expected.createdStamp.map(_.date)
      actual.length mustBe None
      actual.stream mustBe None

      val md = actual.metadata
      md.owner mustBe expected.owner
      md.accessibleBy.headOption.map(_.id.value) mustBe expected.collection.map(_.value)
      md.fid mustBe expected.fid
      md.uploadedBy mustBe expected.createdStamp.map(_.by)
      md.version mustBe 1
      md.isFolder mustBe Some(true)
      md.path mustBe expected.path
      md.description mustBe expected.description
      md.lock mustBe expected.lock

      val e = md.extraAttributes.value
      e.getAs[Boolean]("published") mustBe Some(expected.published)
      e.getAs[String]("documentMedium") mustBe expected.documentMedium
      e.getAs[DateTime]("closedDate") mustBe expected.closedStamp.map(_.date)
      e.getAs[String]("closedBy") mustBe expected.closedStamp.map(_.by.value)
    }

    "be implicitly converted from a symbiotic Folder" in {
      val f: Folder       = expected
      val actual: Archive = f

      actual mustBe expected
    }

  }

  "An ArchivePart" should {

    val expected = createArchiveFolderItem(
      fid = FileId.createOpt(),
      title = "test archive part",
      description = Some("this is a test")
    )(ArchivePart.apply)

    "be implicitly converted to a symbiotic Folder" in {
      val actual: Folder = expected

      actual.id.map(_.toString) mustBe expected.id.map(_.asString)
      actual.filename mustBe expected.title
      actual.fileType mustBe Some(ArchivePart.FolderType)
      actual.uploadDate mustBe expected.createdStamp.map(_.date)
      actual.length mustBe None
      actual.stream mustBe None

      val md = actual.metadata
      md.owner mustBe expected.owner
      md.accessibleBy.headOption.map(_.id.value) mustBe expected.collection.map(_.value)
      md.fid mustBe expected.fid
      md.uploadedBy mustBe expected.createdStamp.map(_.by)
      md.version mustBe 1
      md.isFolder mustBe Some(true)
      md.path mustBe expected.path
      md.description mustBe expected.description
      md.lock mustBe expected.lock

      val e = md.extraAttributes.value
      e.getAs[Boolean]("published") mustBe Some(expected.published)
      e.getAs[String]("documentMedium") mustBe expected.documentMedium
      e.getAs[DateTime]("closedDate") mustBe expected.closedStamp.map(_.date)
      e.getAs[String]("closedBy") mustBe expected.closedStamp.map(_.by.value)
    }

    "be implicitly converted from a symbiotic Folder" in {
      val f: Folder           = expected
      val actual: ArchivePart = f

      actual mustBe expected
    }

  }

  "An ArchiveFolder" should {

    val expected = createArchiveFolderItem(
      fid = FileId.createOpt(),
      title = "test archive folder",
      description = Some("this is a test")
    )(ArchiveFolder.apply)

    "be implicitly converted to symbiotic Folder" in {
      val actual: Folder = expected

      actual.id.map(_.toString) mustBe expected.id.map(_.asString)
      actual.filename mustBe expected.title
      actual.fileType mustBe Some(ArchiveFolder.FolderType)
      actual.uploadDate mustBe expected.createdStamp.map(_.date)
      actual.length mustBe None
      actual.stream mustBe None

      val md = actual.metadata
      md.owner mustBe expected.owner
      md.accessibleBy.headOption.map(_.id.value) mustBe expected.collection.map(_.value)
      md.fid mustBe expected.fid
      md.uploadedBy mustBe expected.createdStamp.map(_.by)
      md.version mustBe 1
      md.isFolder mustBe Some(true)
      md.path mustBe expected.path
      md.description mustBe expected.description
      md.lock mustBe expected.lock

      val e = md.extraAttributes.value
      e.getAs[Boolean]("published") mustBe Some(expected.published)
      e.getAs[String]("documentMedium") mustBe expected.documentMedium
      e.getAs[DateTime]("closedDate") mustBe expected.closedStamp.map(_.date)
      e.getAs[String]("closedBy") mustBe expected.closedStamp.map(_.by.value)
    }

    "be implicitly converted from a symbiotic Folder" in {
      val f: Folder             = expected
      val actual: ArchiveFolder = f

      actual mustBe expected
    }

  }

}
