package services

import com.google.inject.Inject
import play.api.Logger
import repositories.dao.SampleObjectDao

class SampleObjectService @Inject() (
    val sampleObjectDao: SampleObjectDao
) {

  val logger = Logger(classOf[SampleObjectService])

}
