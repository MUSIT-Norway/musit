package services.storage

import com.google.inject.Inject
import repositories.storage.dao.events.ControlDao

class ControlService @Inject()(
    val controlDao: ControlDao
) {}
