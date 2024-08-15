package uk.gov.homeoffice.drt.db

import org.specs2.mutable.Specification
import org.specs2.specification.BeforeEach
import slick.dbio.DBIO
import slick.jdbc.PostgresProfile.api._
import uk.gov.homeoffice.drt.db.dao.ABFeatureDao
import uk.gov.homeoffice.drt.db.tables.ABFeatureRow

import java.sql.Timestamp
import java.time.Instant
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class ABFeatureDaoSpec extends Specification with BeforeEach {

  sequential

  lazy val db = TestDatabase.db

  override def before = {
    Await.result(
      db.run(DBIO.seq(
        TestDatabase.abFeatureTable.schema.dropIfExists,
        TestDatabase.abFeatureTable.schema.createIfNotExists)
      ), 2.second)
  }

  def getABFeatureRow() = {
    ABFeatureRow(email = "test@test.com",
      functionName = "feedback",
      presentedAt = new Timestamp(Instant.now().toEpochMilli),
      abVersion = "A")
  }

  "ABFeatureDao" should {
    "should return a list of AB Features" in {
      val abFeatureDao = ABFeatureDao(TestDatabase.db)
      val abFeatureRow = getABFeatureRow()

      Await.result(abFeatureDao.insertOrUpdate(abFeatureRow), 1.second)
      val abFeatureSelectResult = Await.result(abFeatureDao.getABFeatures, 1.second)

      abFeatureSelectResult.size === 1
      abFeatureSelectResult.head === abFeatureRow
    }

    "should return AB Features for a given functionName" in {
      val abFeatureDao = ABFeatureDao(TestDatabase.db)
      val abFeatureRow = getABFeatureRow()

      val arrivalFeature = abFeatureRow.copy(functionName = "arrival")
      Await.result(abFeatureDao.insertOrUpdate(abFeatureRow), 1.second)
      Await.result(abFeatureDao.insertOrUpdate(arrivalFeature), 1.second)

      val abFeatureSelectResult = Await.result(abFeatureDao.getABFeatureByFunctionName("arrival"), 1.second)

      abFeatureSelectResult.size === 1
      abFeatureSelectResult.head === arrivalFeature
    }

    "should return AB Features for a given functionName and email" in {
      val abFeatureDao = ABFeatureDao(TestDatabase.db)
      val abFeatureRow = getABFeatureRow()

      val arrivalFeature = abFeatureRow.copy(functionName = "arrival",email="test1@test.com")
      Await.result(abFeatureDao.insertOrUpdate(abFeatureRow), 1.second)
      Await.result(abFeatureDao.insertOrUpdate(arrivalFeature), 1.second)

      val abFeatureSelectResult = Await.result(abFeatureDao.getABFeaturesByEmailForFunction("test1@test.com","arrival"), 1.second)

      abFeatureSelectResult.size === 1
      abFeatureSelectResult.head === arrivalFeature
    }
  }
}
