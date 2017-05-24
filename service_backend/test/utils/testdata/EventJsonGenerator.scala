package utils.testdata

import no.uio.musit.models.ActorId

object EventJsonGenerator {

  def baseEventJson(doneBy: ActorId, eventType: String, regDay: Int) = {
    s""""doneDate" : "2016-09-${regDay}T09:17:46+02:00",
        |"doneBy" : "${doneBy.asString}",
        |"eventType" : "$eventType"""".stripMargin
  }

  def controlJson(doneBy: ActorId, regDay: Int) = {
    s"""{
        |  ${baseEventJson(doneBy, "Control", regDay)},
        |  "temperature" : ${ctrlSubFromToJson("temperature", ok = false)},
        |  "alcohol": ${ctrlSubFromToJson("alcohol", ok = false)},
        |  "cleaning": ${ctrlSubStringJson("cleaning", ok = false)},
        |  "pest": ${ctrlSubPestJson(ok = false)}
        |}""".stripMargin
  }

  def observationJson(doneBy: ActorId, regDay: Int) = {
    s"""{
        |  ${baseEventJson(doneBy, "Observation", regDay)},
        |  "temperature" : ${obsFromToJson("temperature")},
        |  "alcohol": ${obsFromToJson("alcohol")},
        |  "cleaning": ${obsStringJson("cleaning")},
        |  "pest": $obsPestJson
        |}""".stripMargin
  }

  def observation(propName: String, ok: Boolean)(obsJson: (String) => String) = {
    if (!ok) {
      s""""observation" : ${obsJson(propName)}"""
    } else {
      ""
    }
  }

  def ctrlSubStringJson(propName: String, ok: Boolean = true) = {
    val maybeMotivates = observation(propName, ok)(obsStringJson)
    s"""{
        |  "ok" : $ok,
        |  $maybeMotivates
        |}""".stripMargin
  }

  def ctrlSubFromToJson(propName: String, ok: Boolean = true) = {
    val maybeObservation = observation(propName, ok)(obsFromToJson)
    s"""{
        |  "ok" : $ok,
        |  $maybeObservation
        |}""".stripMargin
  }

  def ctrlSubPestJson(ok: Boolean = true) = {
    val maybeMotivates = {
      if (!ok) {
        s""""observation" : $obsPestJson"""
      } else {
        ""
      }
    }
    s"""{
        |  "ok" : $ok,
        |  $maybeMotivates
        |}""".stripMargin
  }

  def obsStringJson(propName: String) = {
    s"""{
        |  "note": "This is an observation $propName note",
        |  "$propName" : "The value for $propName is a String"
        |}""".stripMargin
  }

  def obsFromToJson(propName: String) = {
    s"""{
        |  "note": "This is an observation $propName note",
        |  "range": {
        |    "from" : 12.32,
        |    "to" : 24.12
        |  }
        |}""".stripMargin
  }

  def obsPestJson = {
    s"""{
        |  "note": "This is an observation pest note",
        |  "identification" : "termites",
        |  "lifecycles" : [ {
        |    "stage" : "mature colony",
        |    "quantity" : 100
        |  }, {
        |    "stage" : "new colony",
        |    "quantity" : 4
        |  } ]
        |}""".stripMargin
  }

}
