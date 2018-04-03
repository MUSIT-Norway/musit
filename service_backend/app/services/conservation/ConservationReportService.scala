package services.conservation

import com.google.inject.Inject
import models.conservation.{ConditionCode, TreatmentKeyword, TreatmentMaterial}
import models.conservation.events._
import models.musitobject.MusitObject
import no.uio.musit.MusitResults.MusitSuccess
import no.uio.musit.functional.FutureMusitResult
import no.uio.musit.models.{EventId, FileId, MuseumId}
import no.uio.musit.security.AuthenticatedUser
import play.api.Logger
import services.actor.ActorService
import services.musitobject.ObjectService
import no.uio.musit.functional.Extensions._
import no.uio.musit.functional.FutureMusitResult

import scala.concurrent.{ExecutionContext, Future}
import scalatags.Text
import scalatags.Text.all._ //{body, div, h1, html, p}
import org.joda.time.DateTime

import org.joda.time.format.{DateTimeFormat}
import org.joda.time.DateTime.parse

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

    } else None
  }

  private def createTableOfObjects(
      affectedThingsDetails: Seq[MusitObject]
  ): Text.TypedTag[String] = {
    table(
      tr(
        th("Museumsnummer"),
        th("Antall"),
        th("Gjenstandstype(?)")
      ),
      affectedThingsDetails.map(
        obj =>
          tr(
            td(getMusno(obj.museumNo.value, obj.subNo.map(x => x.value))),
            td("1"),
            td(obj.term)
        )
      )
    )
  }

  private def getSuvEventCommonAttributes(
      event: ConservationReportSubEvent,
      addObjectTable: Boolean
  ): Text.TypedTag[String] = {
    span(
      h3(getEventTypeName(event.eventType)),
      if (addObjectTable) getAffectedThingsDetails(event.affectedThingsDetails) else "",
      div(),
      getEventId(event),
      getActorsAndRoles(event),
      getNote(event) //,
      //getDocuments(event.documents)
    )
  }

  private def getAffectedThingsDetails(
      affectedThingsDetails: Seq[MusitObject]
  ): Text.TypedTag[String] = {
    createTableOfObjects(affectedThingsDetails)
  }

  private def getDocuments(documents: Option[Seq[FileId]]): Text.TypedTag[String] =
    documents match {
      case Some(documents) =>
        div("Vedlegg: " + documents.mkString(", "))
      case None => div("Vedlegg: ")
    }
  private def getActorsAndRoles(
      event: ConservationReportSubEvent
  ): Text.all.SeqFrag[Option[Text.TypedTag[String]]] = {

    val dateFormatter = DateTimeFormat.forPattern("dd.mm.yyyy");

    event.actorsAndRolesDetails.map(
      a =>
        a.role.map(r => {
          if (a.date == null)
            div(r.noRole + ":  " + a.actor.getOrElse("(mangler navn)"))
          else {
            span(
              div(r.noRole + ":  " + a.actor.getOrElse("(mangler navn)")),
              div(
                r.noRole + " dato:  " +
                  getFormattedDate(a.date)
              )
            )
          }
        })
    )
  }

  private def getFormattedDate(
      date: Option[DateTime]
  ): String = date match {
    case Some(date) =>
      date.getDayOfMonth().toString + "." + date.getMonthOfYear().toString + "." + date
        .getYear()
        .toString
    case None => "(mangler dato)"
  }

  private def getEventId(event: ConservationReportSubEvent): Text.TypedTag[String] = {
    div("HID: " + event.id.getOrElse(""))
  }
  private def getNote(event: ConservationReportSubEvent): Text.TypedTag[String] = {
    div("Merknad: " + event.note.getOrElse(""))
  }

  private def titleCase(s: String) =
    s.head.toUpper + s.tail.toLowerCase

  private def getEventTypeName(
      conservationType: Option[ConservationType]
  ): String = conservationType match {
    case Some(conservationType) => titleCase(conservationType.noName)
    case None                   => ""
  }

  def getTreatmentData(event: TreatmentReport, addObjectTable: Boolean) = {
    def getKeywords(keywords: Seq[TreatmentKeyword]): String = {
      if (keywords.length > 0)
        "Stikkord: " + keywords.map(k => k.noTerm).mkString(", ")
      else ""
    }
    def getMaterialsDetails(materials: Seq[TreatmentMaterial]): String = {
      if (materials.length > 0)
        "Materialbruk: " + materials.map(k => k.noTerm).mkString(", ")
      else ""
    }

    div(
      getSuvEventCommonAttributes(event, addObjectTable),
      div(getKeywords(event.keywordsDetails)),
      div(getMaterialsDetails(event.materialsDetails))
    )
  }

  def getMeasurementDeterminationData(
      event: MeasurementDeterminationReport,
      addObjectTable: Boolean
  ) = {

    def getMeasurementData(
        measurementData: Option[MeasurementData]
    ): String = {
      def nvl(key: String, value: Option[Any]) = {
        value match {
          case Some(v) => ", " + key + ": " + v
          case None    => ""
        }
      }
      measurementData match {
        case Some(measurementData) =>
          ("Mål: " +
            nvl("vekt(gr)", measurementData.weight)
            + nvl("lengde(mm)", measurementData.length)
            + nvl("bredde(mm)", measurementData.width)
            + nvl("tykkelse(mm)", measurementData.thickness)
            + nvl("høyde(mm)", measurementData.height)
            + nvl("største lengde(mm)", measurementData.largestLength)
            + nvl("største bredde(mm)", measurementData.largestWidth)
            + nvl("største tykkelse (mm)", measurementData.largestThickness)
            + nvl("største høyde (mm)", measurementData.largestHeight)
            + nvl("diameter (mm)", measurementData.diameter)
            + nvl("tverrmål (mm)", measurementData.tverrmaal)
            + nvl("største mål (mm)", measurementData.largestMeasurement)
            + nvl("annet mål", measurementData.measurement)
            + nvl("antall", measurementData.quantity)
            + nvl("usikkerhet", measurementData.quantitySymbol)
            + nvl("antall fragment", measurementData.fragmentQuantity))
        case None => ""
      }
    }

    div(
      getSuvEventCommonAttributes(event, addObjectTable),
      div(getMeasurementData(event.measurementData))
    )
  }

  def getTechnicalDescriptionData(
      event: TechnicalDescriptionReport,
      addObjectTable: Boolean
  ) =
    div(getSuvEventCommonAttributes(event, addObjectTable))

  def getStorageAndHandlingData(
      event: StorageAndHandlingReport,
      addObjectTable: Boolean
  ) =
    div(
      getSuvEventCommonAttributes(event, addObjectTable),
      div("Relativ fuktighet(%): " + event.relativeHumidity),
      div("Temperatur(°C): " + event.temperature),
      div("Lux: " + event.lightLevel),
      div("UVnivå(μW/lumen): " + event.uvLevel)
    )

  def getHseRiskAssessmentData(event: HseRiskAssessmentReport, addObjectTable: Boolean) =
    div(getSuvEventCommonAttributes(event, addObjectTable))

  def getConditionAssessmentData(
      event: ConditionAssessmentReport,
      addObjectTable: Boolean
  ) = {
    def getConditionCode(
        conditionCode: Option[ConditionCode]
    ): String = conditionCode match {
      case Some(conditionCode) => titleCase(conditionCode.noCondition)
      case None                => ""
    }
    div(
      getSuvEventCommonAttributes(event, addObjectTable),
      div("Tilstandskode: " + getConditionCode(event.conditionCodeDetails))
    )
  }

  def getReportData(event: ReportReport, addObjectTable: Boolean) =
    div(getSuvEventCommonAttributes(event, addObjectTable))

  def getMaterialDeterminationData(
      event: MaterialDeterminationReport,
      addObjectTable: Boolean
  ) = {
    div(getSuvEventCommonAttributes(event, addObjectTable)) // had to add material details later
  }
  def getNoteData(event: NoteReport, addObjectTable: Boolean) =
    div(getSuvEventCommonAttributes(event, addObjectTable))

  def getEventData(
      event: ConservationReportSubEvent,
      addObjectTable: Boolean
  ): Text.TypedTag[String] = {
    ConservationEventType(event.eventTypeId) match {
      case Some(eventType) =>
        eventType match {
          case Treatment => {
            getTreatmentData(event.asInstanceOf[TreatmentReport], addObjectTable)
          }
          case MeasurementDetermination => {
            getMeasurementDeterminationData(
              event.asInstanceOf[MeasurementDeterminationReport],
              addObjectTable
            )
          }
          case TechnicalDescription => {
            getTechnicalDescriptionData(
              event.asInstanceOf[TechnicalDescriptionReport],
              addObjectTable
            )
          }
          case StorageAndHandling => {
            getStorageAndHandlingData(
              event.asInstanceOf[StorageAndHandlingReport],
              addObjectTable
            )
          }
          case HseRiskAssessment => {
            getHseRiskAssessmentData(
              event.asInstanceOf[HseRiskAssessmentReport],
              addObjectTable
            )
          }
          case ConditionAssessment => {
            getConditionAssessmentData(
              event.asInstanceOf[ConditionAssessmentReport],
              addObjectTable
            )
          }
          case Report => {
            getReportData(event.asInstanceOf[ReportReport], addObjectTable)
          }
          case MaterialDetermination => {
            getMaterialDeterminationData(
              event.asInstanceOf[MaterialDeterminationReport],
              addObjectTable
            )
          }
          case Note => {
            getNoteData(event.asInstanceOf[NoteReport], addObjectTable)
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
      div(conservationReport.eventsDetails.map { event =>
        div(getEventData(event, conservationReport.affectedThings.length == 1))
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
