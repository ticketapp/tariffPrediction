package modelsMethods

import javax.inject.Inject

import database.MyPostgresDriver.api._
import database.{MyDBTableDefinitions, MyPostgresDriver}
import logger.LoggerHelper
import models.Counts
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}

import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps
import scala.util.control.NonFatal

class AttendeeMethods @Inject()(protected val dbConfigProvider: DatabaseConfigProvider,
                                implicit val ec: ExecutionContext)
    extends HasDatabaseConfigProvider[MyPostgresDriver]
    with MyDBTableDefinitions
    with LoggerHelper {

  def saveCounts(counts: Counts): Future[Any] = db.run(eventsCounts += counts) recover { case NonFatal(e) =>
    log(e.getMessage)
  }

  def findCountsByFacebookIds(eventFacebookId: String): Future[Option[Counts]] =
    db.run(eventsCounts.filter(_.eventFacebookId === eventFacebookId).result.headOption)
}
