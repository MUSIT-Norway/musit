package services.conservation

import com.google.inject.Inject
import models.conservation.events._
import models.musitobject.MusitObject
import no.uio.musit.MusitResults.MusitSuccess
import no.uio.musit.functional.FutureMusitResult
import no.uio.musit.models.{EventId, MuseumId}
import no.uio.musit.security.AuthenticatedUser
import play.api.Logger
import services.actor.ActorService
import services.musitobject.ObjectService
import no.uio.musit.functional.Extensions._
import no.uio.musit.functional.FutureMusitResult

import scala.concurrent.{ExecutionContext, Future}
import scalatags.Text
import scalatags.Text.all._ //{body, div, h1, html, p}

class ConservationReportService @Inject()(
    implicit
    val conservationService: ConservationService,
    val conservationProcessService: ConservationProcessService,
    val ec: ExecutionContext,
    val objService: ObjectService,
    val actorService: ActorService
) {

  val logger = Logger(classOf[ConservationReportService])

  def getMaterialsForObject(obj: MusitObject): Option[String] = {
    obj.materials.map(os => os.mkString(","))
  }

  def getRow(lbl: String, value: String) = {
    tr(
      td(b(lbl)),
      td(value)
    )
  }
  def getMusno(musno: String, subno: Option[String]) = {
    subno match {
      case Some(s) => musno + "/" + s
      case None    => musno
    }
  }

  def getObjectHTMLFrag(conservationReport: ConservationProcessForReport) = {
    if (conservationReport.affectedThings.length == 1) {
      val obj = conservationReport.affectedThingsDetails.head

      val sub      = obj.subNo.map(x => x.value)
      val museumNo = getMusno(obj.museumNo.value, sub)
      val measuremetEvents = conservationReport.events.filter { event =>
        event.eventTypeId == MeasurementDetermination.eventTypeId
      }

      val measurements = measuremetEvents.map { event =>
        val measureData = event.asInstanceOf[MeasurementDetermination].measurementData
        measureData.map { data =>
          val w = data.weight match {
            case Some(x) => s"Vekt: $x"
            case None    => ""
          }
          val l = data.length match {
            case Some(x) => s"Lengde: $x"
            case None    => ""
          }
          w + " " + l
        } match {
          case Some(x) => x
          case None    => ""
        }
      }

      val treatmentEvents = conservationReport.events.filter { event =>
        event.eventTypeId == Treatment.eventTypeId
      }

      val treatments = treatmentEvents.map { event =>
        val treatmentNote = event.asInstanceOf[Treatment].note
        treatmentNote.getOrElse("")
      }

      def getMeasurements = {
        for {
          m <- measurements
        } yield getRow("Measurement", m)
      }
      def getTreatments = {
        for {
          t <- treatments
        } yield getRow("Behandling", t)
      }
      Some(
        table(
          getRow("Museumsnummer", getMusno(obj.museumNo.value, sub)),
          getRow("Antall", "1"),
          getRow("Gjenstandstype(?)", obj.term),
          getMeasurements,
          getTreatments
        )
      )

    } else
      None
  }

  def getTreatmentData(event: Treatment) = {
    div(
      h3("Behandling"),
      div("Behandling: " + event.note.getOrElse("")),
      event.keywords.map(x => div("Materialbruk:" + x.mkString(",")))
    )
  }
  def getMeasurementDeterminationData(event: MeasurementDetermination) = {
    div(
      h3("Målbestemmelse"),
      div("Målbestemmelse...")
    )
  }

  def getTechnicalDescriptionData(event: TechnicalDescription) = {
    div(
      h3("TechnicalDescription")
    )
  }

  def getStorageAndHandlingData(event: StorageAndHandling) = {
    div(
      h3("StorageAndHandling")
    )
  }
  def getHseRiskAssessmentData(event: HseRiskAssessment) = {
    div(
      h3("HseRiskAssessment")
    )
  }
  def getConditionAssessmentData(event: ConditionAssessment) = {
    div(
      h3("ConditionAssessment")
    )
  }
  def getReportData(event: Report) = {
    div(
      h3("Report")
    )
  }
  def getMaterialDeterminationData(event: MaterialDetermination) = {
    div(
      h3("MaterialDetermination")
    )
  }
  def getNoteData(event: Note) = {
    div(
      h3("Note")
    )
  }

  def getEventData(event: ConservationEvent): Text.TypedTag[String] = {
    ConservationEventType(event.eventTypeId) match {
      case Some(eventType) =>
        eventType match {
          case Treatment => {
            getTreatmentData(event.asInstanceOf[Treatment])
          }
          case MeasurementDetermination => {
            getMeasurementDeterminationData(event.asInstanceOf[MeasurementDetermination])
          }
          case TechnicalDescription => {
            getTechnicalDescriptionData(event.asInstanceOf[TechnicalDescription])
          }
          case StorageAndHandling => {
            getStorageAndHandlingData(event.asInstanceOf[StorageAndHandling])
          }
          case HseRiskAssessment => {
            getHseRiskAssessmentData(event.asInstanceOf[HseRiskAssessment])
          }
          case ConditionAssessment => {
            getConditionAssessmentData(event.asInstanceOf[ConditionAssessment])
          }
          case Report => {
            getReportData(event.asInstanceOf[Report])
          }
          case MaterialDetermination => {
            getMaterialDeterminationData(event.asInstanceOf[MaterialDetermination])
          }
          case Note => {
            getNoteData(event.asInstanceOf[Note])
          }

          case _ => {
            div(
              h3("Ukjent hendelse" + event.getClass.getSimpleName)
            )
          }
        }
      case None => div("")
    }
  }

  def getConservationReportHtml(
      conservationReport: ConservationProcessForReport
  ): FutureMusitResult[Option[String]] = {
//    assert(conservationReport.affectedThings.length==1)

    def getEvents = {
      div(conservationReport.events.map { event =>
        div(getEventData(event))
      })
    }

    val report = html(
      head(
        meta(content := "text/html; charset=UTF-8")
      ),
      body(
        h1("KONSERVERINGSRAPPORT"),
        getObjectHTMLFrag(conservationReport),
        h2("Hendelser"),
        getEvents()
      )
    ).toString()

    val rep1 = scala.xml.XML.loadString(report)
    val xml  = report
    // max width: 80 chars
    // indent:     2 spaces
    val printer = new scala.xml.PrettyPrinter(80, 2)
    val r       = "<!DOCTYPE html>\n" + printer.format(rep1)

    FutureMusitResult(Future(MusitSuccess(Option(r))))
  }

  def getConservationReportAsHTML(mid: MuseumId, collectionId: String, id: EventId)(
      implicit currUser: AuthenticatedUser
  ): FutureMusitResult[Option[String]] = {

    val conservationData =
      conservationProcessService.getConservationReportService(mid, collectionId, id)

    val rep = conservationData.flatMapInsideOption { r =>
      getConservationReportHtml(r)
    }

    rep.map(_.flatten)
  }

}