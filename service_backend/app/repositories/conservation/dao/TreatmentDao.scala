package repositories.conservation.dao

import com.google.inject.{Inject, Singleton}
import models.conservation.{TreatmentKeyword, TreatmentMaterial}
import models.conservation.events.Treatment
import no.uio.musit.MusitResults.{MusitResult, MusitSuccess}
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.{ExecutionContext, Future}
@Singleton
class TreatmentDao @Inject()(
    implicit
    override val dbConfigProvider: DatabaseConfigProvider,
    override val ec: ExecutionContext,
    override val objectEventDao: ObjectEventDao
) extends ConservationEventDao[Treatment] {

  override val logger = Logger(classOf[TreatmentDao])

  import profile.api._

  def getMaterialList: Future[MusitResult[Seq[TreatmentMaterial]]] = {
    db.run(treatmentMaterialTable.result)
      .map(_.map(fromTreatmentMaterialRow))
      .map(MusitSuccess.apply)
      .recover(nonFatal(s"An unexpected error occurred fetching material list"))
  }
  def getKeywordList: Future[MusitResult[Seq[TreatmentKeyword]]] = {
    db.run(treatmentKeywordTable.result)
      .map(_.map(fromTreatmentKeywordRow))
      .map(MusitSuccess.apply)
      .recover(nonFatal(s"An unexpected error occurred fetching material list"))
  }
}
