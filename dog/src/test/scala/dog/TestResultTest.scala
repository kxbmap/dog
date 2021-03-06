package dog

import scalaz._
import scalaz.std.anyVal._
import scalaprops._
import TestGen._
import props._

object TestResultTest extends Dog {

  import TestResult._

  implicit val exnEqual: Equal[Throwable] = new Equal[Throwable] {
    override def equal(a1: Throwable, a2: Throwable) = a1 == a2
  }

  implicit val exn: Gen[Throwable] = Gen.value(new Exception())

  implicit val gen: Gen[TestResult[Int]] = TestGen.testResult[Int]

  val laws = Properties.list(
    scalazlaws.monad.all[TestResult],
    scalazlaws.semigroup.all[TestResult[Int]],
    scalazlaws.equal.all[TestResult[Int]]
  ).toTestCase()
}
