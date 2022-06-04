package windymelt

import org.scalatest._
import org.scalatest.flatspec._
import matchers._

class HelloSpec extends AnyFlatSpec with should.Matchers {
  "Dummy Test" should "behave some great" in {
    true shouldBe true
  }
}
