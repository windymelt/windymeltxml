package windymelt

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import akka.util.ByteString
import org.scalatest._
import org.scalatest.flatspec._

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

import matchers._
import akka.stream.scaladsl.Sink

class ConverterSpec extends AnyFlatSpec with should.Matchers {
  implicit val system = ActorSystem()
  implicit val mat = Materializer(system)

  def byteStringVectorToString(bs: Seq[ByteString]): String =
    bs.map(_.decodeString("utf-8")).mkString
  def runConvert(xml: String): String = {
    val src = Source.single(ByteString(xml))
    var target = "foo"
    val run = Converter
      .connectToXmlConverter(src)
      .runWith(Sink.seq)

    return byteStringVectorToString(Await.result(run, 10 seconds))
  }

  "empty entry" should "be converted" in {
    runConvert("<entry></entry>") shouldBe "\n"
    runConvert("<entry/>") shouldBe "\n"
  }

  "codeblock" should "be converted" in {
    val xml =
      """|<entry>
         |<codeblock>
         |println("Hello, World!")
         |</codeblock>
         |</entry>
         |""".stripMargin
    val expected =
      """|```
         |println("Hello, World!")
         |```
         |""".stripMargin

    val got = runConvert(xml)
    got shouldBe expected
  }

  it should "converted when appered twice" in {
    val xml =
      """|<entry>
         |<codeblock>
         |println("Hello, World!")
         |</codeblock>
         |<codeblock>
         |def x = 42
         |</codeblock>
         |</entry>
         |""".stripMargin
    val expected =
      """|```
         |println("Hello, World!")
         |```
         |
         |```
         |def x = 42
         |```
         |""".stripMargin

    val got = runConvert(xml)
    got shouldBe expected
  }

  it should "converted codeblock with lang attr" in {
    val xml =
      """|<entry>
         |<codeblock lang="scala">
         |println("Hello, World!")
         |</codeblock>
         |</entry>
         |""".stripMargin
    val expected =
      """|```scala
         |println("Hello, World!")
         |```
         |""".stripMargin

    val got = runConvert(xml)
    got shouldBe expected
  }

  "entry with section" should "be converted as ##" in {
    val xml =
      """|<entry>
         |<sec title="foo">
         |bar
         |</sec>
         |</entry>
         |""".stripMargin
    val expected =
      """|## foo
         |bar
         |""".stripMargin
    runConvert(xml) shouldBe expected
  }

  it should "handle multiple section" in {
    val xml =
      """|<entry>
         |<sec title="foo">
         |bar
         |</sec>
         |<sec title="buzz">
         |qux
         |</sec>
         |</entry>
         |""".stripMargin
    val expected =
      """|## foo
         |bar
         |
         |## buzz
         |qux
         |""".stripMargin
    val got = runConvert(xml)
    got shouldBe expected
  }

  it should "handle nested section" in {
    val xml =
      """|<entry>
         |<sec title="1">
         |here is 1
         |<sec title="a">
         |here is 1-a
         |</sec>
         |</sec>
         |<sec title="2">
         |here is 2
         |<sec title="a">
         |here is 2-a
         |</sec>
         |</sec>
         |</entry>
         |""".stripMargin
    val expected =
      """|## 1
         |here is 1
         |
         |### a
         |here is 1-a
         |
         |## 2
         |here is 2
         |
         |### a
         |here is 2-a
         |""".stripMargin
    val got = runConvert(xml)
    got shouldBe expected
  }

  "para" should "make double newline" in {
    val xml =
      """|<entry>
         |日本国民は正当に選挙された国会における代表者を通じて行動し、
         |われらとわれらの子孫のために、諸国民と協和による成果と、
         |わが国全土にわたって自由のもたらす恵沢を確保し、
         |政府の行為によって再び戦争の惨禍が起こることのないようにすることを決意し、
         |ここに主権が国民に存することを宣言し、この憲法を確定する。
         |そもそも国政は国民の厳粛な信託によるものであって、その権威は国民に由来し、
         |その権力は国民の代表者がこれを行使し、その福利は国民がこれを享受する。
         |これは人類普遍の原理であり、この憲法は、かかる原理に基づくものである。
         |われらはこれに反する一切の憲法、法令及び詔勅を排除する。
         |<para/>
         |日本国民は、恒久の平和を念願し、人間相互の関係を支配する崇高な理想を深く自覚するのであって、
         |平和を愛する諸国民の公正と信義を信頼して、われらの安全と生存を保持しようと決意した。
         |われらは平和を維持し、専制と隷従、圧迫と偏狭を地上から永遠に除去しようと努めている国際社会において、
         |名誉ある地位を占めたいと思う。われらは全世界の国民が、ひとしく恐怖と欠乏から免れ、
         |平和の内に生存する権利を有することを確認する。
         |<para/>
         |われらは、いずれの国家も、自国のことのみに専念して他国を無視してはならないのであって、
         |政治道徳の法則は、普遍的なものであり、この法則に従うことは、自国の主権を維持し、
         |他国と対等関係に立とうとする各国の責務であると信ずる。
         |<para/>
         |日本国民は、国家の名誉にかけて、全力をあげて崇高な理想と目的を達成することを誓う。
         |</entry>
         |""".stripMargin
    val expected =
      """|日本国民は正当に選挙された国会における代表者を通じて行動し、
         |われらとわれらの子孫のために、諸国民と協和による成果と、
         |わが国全土にわたって自由のもたらす恵沢を確保し、
         |政府の行為によって再び戦争の惨禍が起こることのないようにすることを決意し、
         |ここに主権が国民に存することを宣言し、この憲法を確定する。
         |そもそも国政は国民の厳粛な信託によるものであって、その権威は国民に由来し、
         |その権力は国民の代表者がこれを行使し、その福利は国民がこれを享受する。
         |これは人類普遍の原理であり、この憲法は、かかる原理に基づくものである。
         |われらはこれに反する一切の憲法、法令及び詔勅を排除する。
         |
         |日本国民は、恒久の平和を念願し、人間相互の関係を支配する崇高な理想を深く自覚するのであって、
         |平和を愛する諸国民の公正と信義を信頼して、われらの安全と生存を保持しようと決意した。
         |われらは平和を維持し、専制と隷従、圧迫と偏狭を地上から永遠に除去しようと努めている国際社会において、
         |名誉ある地位を占めたいと思う。われらは全世界の国民が、ひとしく恐怖と欠乏から免れ、
         |平和の内に生存する権利を有することを確認する。
         |
         |われらは、いずれの国家も、自国のことのみに専念して他国を無視してはならないのであって、
         |政治道徳の法則は、普遍的なものであり、この法則に従うことは、自国の主権を維持し、
         |他国と対等関係に立とうとする各国の責務であると信ずる。
         |
         |日本国民は、国家の名誉にかけて、全力をあげて崇高な理想と目的を達成することを誓う。
         |""".stripMargin
    val got = runConvert(xml)
    got shouldBe expected
  }
}
