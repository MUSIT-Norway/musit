package utils.testdata

import models.conservation.events.{
  ActorRoleDate,
  ConservationProcess,
  MaterialDetermination,
  MaterialInfo
}
import no.uio.musit.models._
import no.uio.musit.time.dateTimeNow

trait ConservationprocessGenerators {
  protected val defaultMid              = Museums.Test.id
  protected val dummyActorId            = ActorId.generate()
  protected val dummyActorById          = ActorId.generate()
  protected val dummyConservationTypeId = EventTypeId(1)

  protected val dummyOrgId = OrgId(315)

  protected val oid1 =
    ObjectUUID.unsafeFromString("2e5037d5-4952-4571-9de2-709eb22b01f0")
  protected val oid2 =
    ObjectUUID.unsafeFromString("4d2e516d-db5f-478e-b409-eac7ff2486e8")
  protected val oid3 =
    ObjectUUID.unsafeFromString("5a928d42-05a6-44db-adef-c6dfe588f016")

  protected val dummyNote = "This is from a SaveConservation command"

  val mid = Museums.Test.id

  def dummyConservationProcess(
      oids: Option[Seq[ObjectUUID]] = Some(Seq(ObjectUUID.generate()))
  ): ConservationProcess = {
    val now = Some(dateTimeNow)
    ConservationProcess(
      id = None,
      eventTypeId = dummyConservationTypeId,
      note = Some(dummyNote),
      caseNumber = None,
      partOf = None,
      actorsAndRoles = Some(
        Seq(
          ActorRoleDate(
            1,
            ActorId.unsafeFromString("d63ab290-2fab-42d2-9b57-2475dfbd0b3c"),
            now
          )
        )
      ),
      affectedThings = oids,
      registeredBy = Some(dummyActorId),
      registeredDate = now,
      updatedBy = None,
      updatedDate = None,
      /*completedBy = None,
      completedDate = None,*/
      events = None,
      isUpdated = Some(false)
    )
  }

  def dummyMaterialDetermination(
      oids: Option[Seq[ObjectUUID]] = Some(
        Seq(ObjectUUID.unsafeFromString("35522dc6-d246-4c7b-a23d-501bb34034f7"))
      )
  ): MaterialDetermination = {
    val now = Some(dateTimeNow)
    MaterialDetermination(
      id = None,
      eventTypeId = MaterialDetermination.eventTypeId,
      registeredBy =
        Some(ActorId.unsafeFromString("41ede78c-a6f6-4744-adad-02c25fb1c97c")),
      registeredDate = now,
      updatedBy = Some(ActorId.unsafeFromString("5224f873-5fe1-44ec-9aaf-b9313db410c6")),
      updatedDate = now,
      /* completedBy = None,
      completedDate = None,*/
      partOf = None,
      note = Some("hurra note"),
      actorsAndRoles = Some(
        Seq(
          ActorRoleDate(
            1,
            ActorId.unsafeFromString("5224f873-5fe1-44ec-9aaf-b9313db410c6"),
            now
          ),
          ActorRoleDate(
            1,
            ActorId.unsafeFromString("41ede78c-a6f6-4744-adad-02c25fb1c97c"),
            now
          )
        )
      ),
      affectedThings = oids,
      documents = Some(
        Seq(FileId.unsafeFromString("d63ab290-2fab-42d2-9b57-2475dfbd0b3c"))
      ),
      materialInfo = Some(Seq(MaterialInfo(1, Some("veldig spes materiale"), Some(1)))),
      isUpdated = Some(false)
    )
  }

}
