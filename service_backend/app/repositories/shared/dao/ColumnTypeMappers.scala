package repositories.shared.dao

import models.analysis.AnalysisStatuses.AnalysisStatus
import models.analysis.LeftoverSamples.LeftoverSample
import models.analysis.SampleStatuses.SampleStatus
import models.analysis.SampleTypeId
import models.analysis.events.{Category, EventCategories}
import models.loan.{LoanEventTypes, LoanType}
import models.storage.nodes.StorageType
import no.uio.musit.repositories.{BaseColumnTypeMappers, ColumnTypesImplicits}
import play.api.db.slick.HasDatabaseConfig
import slick.jdbc.JdbcProfile

trait ColumnTypeMappers extends BaseColumnTypeMappers with ColumnTypesImplicits {
  self: HasDatabaseConfig[JdbcProfile] =>

  import columnTypes._
  import profile.api.{BaseColumnType, MappedColumnType}

  implicit val storageTypeMapper: BaseColumnType[StorageType] =
    MappedColumnType.base[StorageType, String](
      storageType => storageType.entryName,
      string => StorageType.withName(string)
    )

  implicit val sampleTypeIdMapper: BaseColumnType[SampleTypeId] =
    MappedColumnType.base[SampleTypeId, Long](
      etid => etid.underlying,
      lid => SampleTypeId.fromLong(lid)
    )

  implicit val categoryMapper: BaseColumnType[Category] =
    MappedColumnType.base[Category, Int](
      cat => cat.id,
      catId => EventCategories.unsafeFromId(catId)
    )

  implicit val sampleStatusMapper: BaseColumnType[SampleStatus] =
    MappedColumnType.base[SampleStatus, Int](
      ssid => ssid.key,
      intId => SampleStatus.unsafeFromInt(intId)
    )

  implicit val loanTypeMapper: BaseColumnType[LoanType] =
    MappedColumnType.base[LoanType, Long](
      loanType => loanType.id,
      longId => LoanEventTypes.unsafeFromId(longId)
    )

  implicit val leftoverSampleMapper: BaseColumnType[LeftoverSample] =
    MappedColumnType.base[LeftoverSample, Int](
      rm => rm.key,
      intId => LeftoverSample.unsafeFromInt(intId)
    )

  implicit val analysisStatusMapper: BaseColumnType[AnalysisStatus] =
    MappedColumnType.base[AnalysisStatus, Int](
      st => st.key,
      key => AnalysisStatus.unsafeFromInt(key)
    )

}
