package services

import com.google.inject.{Inject, Singleton}
import net.scalytica.symbiotic.core.DocManagementService
import play.api.Logger

import scala.concurrent.ExecutionContext

@Singleton
class ModuleAttachmentsService @Inject()(
    implicit
    val ec: ExecutionContext,
    val dmService: DocManagementService
) {

  private val log = Logger(getClass)



}
