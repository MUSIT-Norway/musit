package services.elasticsearch

import services.elasticsearch.client.models._

object EventIndexConfig {

  val analysisType           = "analysis"
  val analysisCollectionType = "analysisCollection"
  val sampleType             = "sample"

  private val analysis = IndexMapping(
    name = analysisType,
    parent = Some(analysisCollectionType),
    properties = Set(
      TextField("id")
    )
  )

  private val sample = IndexMapping(
    name = sampleType,
    properties = Set(
      TextField("id"),
      actorSearchStamp("doneBy"),
      actorSearchStamp("registeredBy"),
      TextField("objectId"),
      TextField("sampleObjectId")
      //externalLinks
    )
  )

  private val analysisCollection = IndexMapping(
    name = analysisCollectionType,
    properties = Set(
      TextField("id"),
      IntegerField("analysisTypeId"),
      actorSearchStamp("doneBy"),
      actorSearchStamp("registeredBy"),
      actorStamp("responsible"),
      actorStamp("administrator"),
      actorSearchStamp("updatedBy"),
      actorSearchStamp("completedBy"),
      TextField("note"),
      ObjectField(
        "extraAttributes",
        Set(
          // todo: verify and add missing fields in extraAttributes
          TextField("method"),
          TextField("types")
        )
      ),
      ObjectField(
        "result",
        Set(
          // todo: add missing fields in result
          size
        )
      ),
//      restriction
      TextField("reason"),
      IntegerField("status"),
      //caseNumbers
      IntegerField("orgId")
    )
  )

  val config = ElasticsearchConfig(Set(analysis, analysisCollection, sample))

  private def size = {
    ObjectField(
      "size",
      Set(
        TextField("unit"),
        DoubleField("value")
      )
    )
  }

  def actorSearchStamp(name: String) =
    ObjectField(name, Set(TextField("id"), DateField("date"), TextField("name")))

  def actorStamp(name: String) =
    ObjectField(name, Set(TextField("id"), TextField("name")))
}
