import scalaz._
import scalaz.concurrent.Task
import java.util.concurrent.TimeoutException

package object dog {

  type TestCase[A] = Kleisli[TestResult, Endo[Param], A]

  object TestCase {

    def apply[A](result: => TestResult[A]): TestCase[A] =
      Kleisli.kleisli((paramEndo: Endo[Param]) => try {
        val p = paramEndo(Param.default)
        (p.executorService match {
          case Some(s) => Task(result)(s)
          case None => Task(result)
        }).runFor(p.timeout)
      } catch {
        case e: TimeoutException => TestResult.error[A](List(e), List())
        case e: Throwable => TestResult.error[A](List(e), List())
      })

    def ok[A](value: A): TestCase[A] = apply(TestResult(value))

    def delay[A](test: => TestCase[A]): TestCase[A] = test

    def fixture(f: () => Unit): TestCase[Unit] = {
      f()
      ok(())
    }
  }

  implicit def toTestCase[A](result: AssertionResult[A]): TestCase[A] =
    TestCase(AssertionResult.toTestResult(result))

  implicit class TestCaseSyntax[A] private[dog](val self: TestCase[A]) {

    def skip(reason: String): TestCase[A] = TestCase(
      Done(NonEmptyList.nel(-\/(NotPassedCause.skip(reason)), List()))
    )

    def +>(result: AssertionResult[A]): TestCase[A] = self.mapK(_ +> result)
  }

  type AssertionResult[A] = NotPassedCause \/ A

  type AssertionNel[A] = NonEmptyList[AssertionResult[A]]

  object AssertionResult {

    def onlyNotPassed[A](xs: AssertionNel[A]): List[NotPassedCause] =
      xs.list.collect { case -\/(x) => x }

    val toTestResult = new (AssertionResult ~> TestResult) {
      def apply[A](r: AssertionResult[A]): TestResult[A] = r match {
        case \/-(v) => TestResult(v)
        case -\/(c) => TestResult.nel(-\/(c))
      }
    }
  }

  implicit class AssertionResultSyntax[A] private[dog](val self: AssertionResult[A]) {

    def skip(reason: String): AssertionResult[A] = -\/(NotPassedCause.skip(reason))

    def +>(result: => AssertionResult[A]): TestCase[A] = TestCase((self, result) match {
      case (\/-(_), \/-(v)) => TestResult(v)
      case (\/-(_), p @ -\/(_)) => TestResult.nel(p)
      case (p @ -\/(_), \/-(_)) => TestResult.nel(p)
      case (p1 @ -\/(_), p2 @ -\/(_)) => TestResult.nel(p1, p2)
    })
  }
}
