package models.document

import net.scalytica.symbiotic.api.types._

object Implicits {

  private[this] def folderAsArchiveFolderItem(f: Folder): Option[ArchiveFolderItem] = {
    f.fileType.flatMap {
      case ArchiveRoot.FolderType   => Some(f: ArchiveRoot)
      case Archive.FolderType       => Some(f: Archive)
      case ArchivePart.FolderType   => Some(f: ArchivePart)
      case ArchiveFolder.FolderType => Some(f: ArchiveFolder)
      case GenericFolder.FolderType => Some(f: GenericFolder)
      case _                        => None
    }.orElse {
      if (Path.root == f.flattenPath) Some(f: ArchiveRoot)
      else None
    }
  }

  implicit def managedFileToArchiveItem(mf: ManagedFile): ArchiveItem = {
    mf match {
      case f: Folder => folder2ArchiveFolderItem(f)
      case f: File   => f: ArchiveDocument
    }
  }

  implicit def managedFileSeqToArchiveItemSeq(
      smf: Seq[ManagedFile]
  ): Seq[ArchiveItem] = smf.map(mf => mf: ArchiveItem)

  @throws(classOf[IllegalArgumentException])
  implicit def folder2ArchiveFolderItem(f: Folder): ArchiveFolderItem = {
    folderAsArchiveFolderItem(f).getOrElse(
      throw new IllegalArgumentException(
        "The folder cannot be converted to an ArchiveFolderItem because " +
          s"folder type is ${f.fileType}"
      )
    )
  }

  implicit def optFolder2ArchiveFolderItem(
      of: Option[Folder]
  ): Option[ArchiveFolderItem] = of.flatMap(folderAsArchiveFolderItem)

  implicit def archiveFolderItemAsFolder(afi: ArchiveFolderItem): Folder = {
    afi match {
      case a: ArchiveRoot   => ArchiveRoot.archiveRoot2SymbioticRoot(a)
      case a: Archive       => Archive.archive2SymbioticFolder(a)
      case a: ArchivePart   => ArchivePart.archivePart2SymbioticFolder(a)
      case a: ArchiveFolder => ArchiveFolder.archiveFolder2SymbioticFolder(a)
      case a: GenericFolder => GenericFolder.genericFolder2SymbioticFolder(a)
    }
  }

}
