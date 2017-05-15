package models.analysis.events

import models.analysis.events.AnalysisExtras.ElementalAASAttributes.ElementalAAS
import models.analysis.events.AnalysisExtras.ElementalICPAttributes.ElementalICP
import models.analysis.events.AnalysisExtras.IsotopeAttributes.IsotopeAnalysisType
import models.analysis.events.AnalysisExtras.MicroscopyAttributes.MicroscopyMethod
import models.analysis.events.AnalysisExtras.TomographyAttributes.TomographyMethod
import play.api.libs.json._

object AnalysisExtras {

  sealed trait ExtraAttributes

  object ExtraAttributes {

    private val discriminator = "type"

    implicit val reads: Reads[ExtraAttributes] = Reads { js =>
      (js \ discriminator).as[String] match {
        case MicroscopyAttributes.typeName   => MicroscopyAttributes.format.reads(js)
        case TomographyAttributes.typeName   => TomographyAttributes.format.reads(js)
        case IsotopeAttributes.typeName      => IsotopeAttributes.format.reads(js)
        case ElementalICPAttributes.typeName => ElementalICPAttributes.format.reads(js)
        case ElementalAASAttributes.typeName => ElementalAASAttributes.format.reads(js)
        case ExtractionAttributes.typeName   => ExtractionAttributes.format.reads(js)
      }
    }

    implicit val writes: Writes[ExtraAttributes] = Writes {
      case mia: MicroscopyAttributes =>
        MicroscopyAttributes.format.writes(mia).as[JsObject] ++ Json.obj(
          discriminator -> MicroscopyAttributes.typeName
        )

      case toa: TomographyAttributes =>
        TomographyAttributes.format.writes(toa).as[JsObject] ++ Json.obj(
          discriminator -> TomographyAttributes.typeName
        )

      case isa: IsotopeAttributes =>
        IsotopeAttributes.format.writes(isa).as[JsObject] ++ Json.obj(
          discriminator -> IsotopeAttributes.typeName
        )

      case eia: ElementalICPAttributes =>
        ElementalICPAttributes.format.writes(eia).as[JsObject] ++ Json.obj(
          discriminator -> ElementalICPAttributes.typeName
        )

      case eaa: ElementalAASAttributes =>
        ElementalAASAttributes.format.writes(eaa).as[JsObject] ++ Json.obj(
          discriminator -> ElementalAASAttributes.typeName
        )

      case exa: ExtractionAttributes =>
        ExtractionAttributes.format.writes(exa).as[JsObject] ++ Json.obj(
          discriminator -> ExtractionAttributes.typeName
        )
    }

  }

  case class MicroscopyAttributes(method: MicroscopyMethod) extends ExtraAttributes

  object MicroscopyAttributes {

    val typeName = "MicroscopyAttributes"

    sealed trait MicroscopyMethod {
      val id: Int
      val enLabel: String
      val noLabel: String
    }

    object MicroscopyMethod {
      implicit val reads: Reads[MicroscopyMethod] = Reads { js =>
        // scalastyle:off line.size.limit
        js.as[Int] match {
          case LightPolarization.id    => JsSuccess(LightPolarization)
          case ScanningElectron.id     => JsSuccess(ScanningElectron)
          case TransmissionElectron.id => JsSuccess(TransmissionElectron)
          case XRay.id                 => JsSuccess(XRay)
          case err                     => JsError(JsPath, s"$err is not a recognized microscopy method")
        }
        // scalastyle:on line.size.limit
      }

      implicit val writes: Writes[MicroscopyMethod] = Writes {
        case LightPolarization    => JsNumber(LightPolarization.id)
        case ScanningElectron     => JsNumber(ScanningElectron.id)
        case TransmissionElectron => JsNumber(TransmissionElectron.id)
        case XRay                 => JsNumber(XRay.id)
      }
    }

    case object LightPolarization extends MicroscopyMethod {
      override val id      = 1
      override val enLabel = "Light/polarization microscopy"
      override val noLabel = "Lys-/polarisasjonsmikroskopi"
    }

    case object ScanningElectron extends MicroscopyMethod {
      override val id      = 2
      override val enLabel = "Scanning electron microscopy (SEM)"
      override val noLabel = "Scanning elektronmikroskopi (SEM)"
    }

    case object TransmissionElectron extends MicroscopyMethod {
      override val id      = 3
      override val enLabel = "Transmission electron microscopy (TEM)"
      override val noLabel = "Transmisjon elektronmikroskopi (TEM)"
    }

    case object XRay extends MicroscopyMethod {
      override val id      = 4
      override val enLabel = "X-ray microscopy"
      override val noLabel = "Røntgenmikroskopi"
    }

    implicit val format: Format[MicroscopyAttributes] = Json.format[MicroscopyAttributes]

  }

  case class TomographyAttributes(method: TomographyMethod) extends ExtraAttributes

  object TomographyAttributes {

    val typeName = "TomographyAttributes"

    sealed trait TomographyMethod {
      val id: Int
      val enLabel: String
      val noLabel: String
    }

    object TomographyMethod {

      implicit val reads: Reads[TomographyMethod] = Reads { js =>
        // scalastyle:off line.size.limit
        js.as[Int] match {
          case Laser3DScan.id           => JsSuccess(Laser3DScan)
          case StructuredLight3DScan.id => JsSuccess(StructuredLight3DScan)
          case ComputerTomography.id    => JsSuccess(ComputerTomography)
          case XRay.id                  => JsSuccess(XRay)
          case NeutronTomography.id     => JsSuccess(NeutronTomography)
          case err                      => JsError(JsPath, s"$err is not a recognized tomography method")
        }
        // scalastyle:on line.size.limit
      }

      implicit val writes: Writes[TomographyMethod] = Writes {
        case Laser3DScan           => JsNumber(Laser3DScan.id)
        case StructuredLight3DScan => JsNumber(StructuredLight3DScan.id)
        case ComputerTomography    => JsNumber(ComputerTomography.id)
        case XRay                  => JsNumber(XRay.id)
        case NeutronTomography     => JsNumber(NeutronTomography.id)
      }
    }

    case object Laser3DScan extends TomographyMethod {
      override val id      = 1
      override val enLabel = "3D scanning, laser"
      override val noLabel = "3D-skanning, laser"
    }

    case object StructuredLight3DScan extends TomographyMethod {
      override val id      = 2
      override val enLabel = "3D scanning, structured light"
      override val noLabel = "3D-skanning, strukturert lys"
    }

    case object ComputerTomography extends TomographyMethod {
      override val id      = 3
      override val enLabel = "Computer tomography (CT) (X-ray tomography)"
      override val noLabel = "Komputertomografi (CT) (Røntgentomografi)"
    }

    case object XRay extends TomographyMethod {
      override val id      = 4
      override val enLabel = "X-ray microscopy"
      override val noLabel = "Røntgenmikroskopi"
    }

    case object NeutronTomography extends TomographyMethod {
      override val id      = 4
      override val enLabel = "Neutron tomography"
      override val noLabel = "Neutrontomografi"
    }

    implicit val format: Format[TomographyAttributes] = Json.format[TomographyAttributes]

  }

  case class IsotopeAttributes(types: Seq[IsotopeAnalysisType]) extends ExtraAttributes

  object IsotopeAttributes {

    val typeName = "IsotopeAttributes"

    sealed trait IsotopeAnalysisType {
      val id: Int
      val enLabel: String
      val noLabel: String
    }

    object IsotopeAnalysisType {
      implicit val reads: Reads[IsotopeAnalysisType] = Reads { js =>
        // scalastyle:off line.size.limit
        js.as[Int] match {
          case Lead_210_Pb.id         => JsSuccess(Lead_210_Pb)
          case Strontium_87Sr_86Sr.id => JsSuccess(Strontium_87Sr_86Sr)
          case StrontiumNeodymium.id  => JsSuccess(StrontiumNeodymium)
          case Carbon_13C_12C.id      => JsSuccess(Carbon_13C_12C)
          case Nitrogen_15N_14N.id    => JsSuccess(Nitrogen_15N_14N)
          case Oxygen_018_016.id      => JsSuccess(Oxygen_018_016)
          case Sulphur_34S_32S.id     => JsSuccess(Sulphur_34S_32S)
          case Hydrogen_2H_1H.id      => JsSuccess(Hydrogen_2H_1H)
          case err                    => JsError(JsPath, s"$err is not a recognized isotope analysis type")
        }
        // scalastyle:on line.size.limit
      }

      implicit val writes: Writes[IsotopeAnalysisType] = Writes {
        case Lead_210_Pb         => JsNumber(Lead_210_Pb.id)
        case Strontium_87Sr_86Sr => JsNumber(Strontium_87Sr_86Sr.id)
        case StrontiumNeodymium  => JsNumber(StrontiumNeodymium.id)
        case Carbon_13C_12C      => JsNumber(Carbon_13C_12C.id)
        case Nitrogen_15N_14N    => JsNumber(Nitrogen_15N_14N.id)
        case Oxygen_018_016      => JsNumber(Oxygen_018_016.id)
        case Sulphur_34S_32S     => JsNumber(Sulphur_34S_32S.id)
        case Hydrogen_2H_1H      => JsNumber(Hydrogen_2H_1H.id)
      }
    }

    case object Lead_210_Pb extends IsotopeAnalysisType {
      override val id      = 1
      override val enLabel = "Lead 210 Pb"
      override val noLabel = "Bly 210 Pb"
    }

    case object Strontium_87Sr_86Sr extends IsotopeAnalysisType {
      override val id      = 2
      override val enLabel = "Strontium 87Sr/86Sr"
      override val noLabel = enLabel
    }

    case object StrontiumNeodymium extends IsotopeAnalysisType {
      override val id      = 3
      override val enLabel = "Strontium/Neodymium (Sr/Nd)"
      override val noLabel = enLabel
    }

    case object Carbon_13C_12C extends IsotopeAnalysisType {
      override val id      = 4
      override val enLabel = "Carbon 13C/12C"
      override val noLabel = "Karbon 13C/12C"
    }

    case object Nitrogen_15N_14N extends IsotopeAnalysisType {
      override val id      = 5
      override val enLabel = "Nitrogen 15N/14N"
      override val noLabel = enLabel
    }

    case object Oxygen_018_016 extends IsotopeAnalysisType {
      override val id      = 6
      override val enLabel = "Oxygen O18/O16"
      override val noLabel = "Oksygen O18/O16"
    }

    case object Sulphur_34S_32S extends IsotopeAnalysisType {
      override val id      = 7
      override val enLabel = "Sulphur 34S/32S"
      override val noLabel = "Svovel 34S/32S"
    }

    case object Hydrogen_2H_1H extends IsotopeAnalysisType {
      override val id      = 8
      override val enLabel = "Hydrogen 2H/1H"
      override val noLabel = enLabel
    }

    implicit val format: Format[IsotopeAttributes] = Json.format[IsotopeAttributes]

  }

  case class ElementalICPAttributes(method: ElementalICP) extends ExtraAttributes

  case object ElementalICPAttributes {

    val typeName = "ElementalICPAttributes"

    sealed trait ElementalICP {
      val id: Int
      val enLabel: String
      val noLabel: String
    }

    object ElementalICP {
      implicit val reads: Reads[ElementalICP] = Reads { jsv =>
        // scalastyle:off line.size.limit
        jsv.as[Int] match {
          case ICP_OES_AES.id => JsSuccess(ICP_OES_AES)
          case ICP_MS.id      => JsSuccess(ICP_MS)
          case ICP_SFMS.id    => JsSuccess(ICP_SFMS)
          case err            => JsError(JsPath, s"$err is not a recognized elemental ICP method")
        }
        // scalastyle:on line.size.limit
      }

      implicit val writes: Writes[ElementalICP] = Writes {
        case ICP_OES_AES => JsNumber(ICP_OES_AES.id)
        case ICP_MS      => JsNumber(ICP_MS.id)
        case ICP_SFMS    => JsNumber(ICP_SFMS.id)
      }
    }

    case object ICP_OES_AES extends ElementalICP {
      override val id      = 1
      override val enLabel = "ICP-OES/ICP-AES"
      override val noLabel = enLabel
    }

    case object ICP_MS extends ElementalICP {
      override val id      = 2
      override val enLabel = "ICP-MS"
      override val noLabel = enLabel
    }

    case object ICP_SFMS extends ElementalICP {
      override val id      = 3
      override val enLabel = "ICP-SFMS"
      override val noLabel = enLabel
    }

    implicit val format: Format[ElementalICPAttributes] =
      Json.format[ElementalICPAttributes]
  }

  case class ElementalAASAttributes(method: ElementalAAS) extends ExtraAttributes

  case object ElementalAASAttributes {

    val typeName = "ElementalAASAttributes"

    sealed trait ElementalAAS {
      val id: Int
      val enLabel: String
      val noLabel: String
    }

    object ElementalAAS {
      implicit val reads: Reads[ElementalAAS] = Reads { jsv =>
        // scalastyle:off line.size.limit
        jsv.as[Int] match {
          case GFAAS.id => JsSuccess(GFAAS)
          case CVAAS.id => JsSuccess(CVAAS)
          case err      => JsError(JsPath, s"$err is not a recognized elemental AAS method")
        }
        // scalastyle:on line.size.limit
      }

      implicit val writes: Writes[ElementalAAS] = Writes {
        case GFAAS => JsNumber(GFAAS.id)
        case CVAAS => JsNumber(CVAAS.id)
      }
    }

    case object GFAAS extends ElementalAAS {
      override val id      = 1
      override val enLabel = "GFAAS"
      override val noLabel = enLabel
    }

    case object CVAAS extends ElementalAAS {
      override val id      = 2
      override val enLabel = "CVAAS"
      override val noLabel = enLabel
    }

    implicit val format: Format[ElementalAASAttributes] =
      Json.format[ElementalAASAttributes]
  }

  case class ExtractionAttributes(
      extractionType: String,
      method: Option[String]
  ) extends ExtraAttributes

  case object ExtractionAttributes {
    val typeName = "ExtractionAttributes"

    implicit val format: Format[ExtractionAttributes] = Json.format[ExtractionAttributes]
  }

}
