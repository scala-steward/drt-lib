package uk.gov.homeoffice.drt.db.dao

import org.scalatest.BeforeAndAfter
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import slick.dbio.DBIO
import slick.jdbc.PostgresProfile.api._
import uk.gov.homeoffice.drt.db.TestDatabase
import uk.gov.homeoffice.drt.db.tables.ABFeatureRow

import java.sql.Timestamp
import java.time.Instant
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class ABFeatureDaoTest extends AnyWordSpec with Matchers with BeforeAndAfter {
  val abFeatureDao: ABFeatureDao = ABFeatureDao(TestDatabase)

  before {
    Await.result(
      TestDatabase.run(DBIO.seq(
        abFeatureDao.table.schema.dropIfExists,
        abFeatureDao.table.schema.createIfNotExists)
      ), 2.second)
  }

  def getABFeatureRow: ABFeatureRow = {
    ABFeatureRow(email = "test@test.com",
      functionName = "feedback",
      presentedAt = new Timestamp(Instant.now().toEpochMilli),
      abVersion = "A")
  }

  "ABFeatureDao" should {
    "should return a list of AB Features" in {
      val abFeatureRow = getABFeatureRow

      Await.result(abFeatureDao.insertOrUpdate(abFeatureRow), 1.second)
      val abFeatureSelectResult = Await.result(abFeatureDao.getABFeatures, 1.second)

      abFeatureSelectResult should ===(Seq(abFeatureRow))
    }

    "should return AB Features for a given functionName" in {
      val abFeatureRow = getABFeatureRow

      val arrivalFeature = abFeatureRow.copy(functionName = "arrival")
      Await.result(abFeatureDao.insertOrUpdate(abFeatureRow), 1.second)
      Await.result(abFeatureDao.insertOrUpdate(arrivalFeature), 1.second)

      val abFeatureSelectResult = Await.result(abFeatureDao.getABFeatureByFunctionName("arrival"), 1.second)

      abFeatureSelectResult should ===(Seq(arrivalFeature))
    }

    "should return AB Features for a given functionName and email" in {
      val abFeatureRow = getABFeatureRow

      val arrivalFeature = abFeatureRow.copy(functionName = "arrival",email="test1@test.com")
      Await.result(abFeatureDao.insertOrUpdate(abFeatureRow), 1.second)
      Await.result(abFeatureDao.insertOrUpdate(arrivalFeature), 1.second)

      val abFeatureSelectResult = Await.result(abFeatureDao.getABFeaturesByEmailForFunction("test1@test.com","arrival"), 1.second)

      abFeatureSelectResult should ===(Seq(arrivalFeature))
    }
  }
}
