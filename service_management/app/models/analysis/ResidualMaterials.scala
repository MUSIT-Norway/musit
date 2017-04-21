package models.analysis

import play.api.libs.json._

object ResidualMaterials {

  sealed trait ResidualMaterial {
    val identity: Int
  }

  object ResidualMaterial {
    def fromInt(i: Int): Option[ResidualMaterial] = i match {
      case NotSpecified.identity        => Some(NotSpecified)
      case NoResidualMaterial.identity  => Some(NoResidualMaterial)
      case HasResidualMaterial.identity => Some(HasResidualMaterial)
      case _                            => None
    }

    @throws(classOf[IllegalArgumentException])
    def unsafeFromInt(i: Int): ResidualMaterial = {
      fromInt(i).getOrElse {
        throw new IllegalArgumentException(s"Unknown residual material $i")
      }
    }

    implicit val reads: Reads[ResidualMaterial] = Reads { jsv =>
      jsv.validate[Int] match {
        case JsSuccess(NotSpecified.identity, _)        => JsSuccess(NotSpecified)
        case JsSuccess(NoResidualMaterial.identity, _)  => JsSuccess(NoResidualMaterial)
        case JsSuccess(HasResidualMaterial.identity, _) => JsSuccess(HasResidualMaterial)
        case JsSuccess(bad, p)                          => JsError(p, s"Unknown residual material code $bad")
        case err: JsError                               => err
      }
    }

    implicit val writes: Writes[ResidualMaterial] = Writes(rm => JsNumber(rm.identity))

  }

  case object NotSpecified extends ResidualMaterial {
    override val identity = 1
  }

  case object NoResidualMaterial extends ResidualMaterial {
    override val identity = 2
  }

  case object HasResidualMaterial extends ResidualMaterial {
    override val identity = 3
  }

}
