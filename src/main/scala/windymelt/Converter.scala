package windymelt

import akka.stream.scaladsl.Flow
import akka.NotUsed
import akka.util.ByteString
import akka.stream.scaladsl.Source
import akka.stream.alpakka.xml.scaladsl.XmlParsing
import scala.collection.mutable.Stack
import akka.stream.alpakka.xml.StartElement
import akka.stream.alpakka.xml.EndElement
import akka.stream.alpakka.xml.TextEvent

object Converter {
  case class Ctx(
      var tagContext: Stack[String] = Stack.empty,
      var textStack: Stack[String] = Stack.empty,
      var isHeadSection: Boolean = true,
      var sectionDepth: Int = 1 // ## から始めたい
  ) {
    def safePop(): String = {
      if (this.textStack.size == 0) ""
      else {
        val popped = this.textStack.pop()
        if (popped == " " || popped.isBlank) {
          ""
        } else {
          popped
        }
      }
    }
    def possibleNewLine() = {
      if (this.isHeadSection) {
        this.isHeadSection = false
        ""
      } else {
        "\n\n"
      }
    }
  }
  val tagMap: Map[String, Tag] = Map(
    "entry" -> tag.Entry,
    "meta" -> tag.Meta,
    "sec" -> tag.Sec,
    "code" -> tag.Code,
    "codeblock" -> tag.Codeblock,
    "para" -> tag.Para,
    "li" -> tag.Li,
    "ul" -> tag.Ul,
    "em" -> tag.Em
  )
  def connectToXmlConverter[A](
      source: Source[ByteString, A]
  ): Source[ByteString, A] = {
    source
      .via(XmlParsing.parser)
      .statefulMapConcat(() => {
        // state
        val ctx = Ctx()

        // agg
        ev =>
          ev match {
            case s: StartElement =>
              tagMap.get(s.localName) match {
                case Some(tg) =>
                  val (_, strs: Seq[String]) = tg.opening(s, ctx)
                  strs
                case None =>
                  println(s"*** Unknown element: ${s.localName}")
                  Seq(ctx.safePop())
              }
            case s: EndElement =>
              tagMap.get(s.localName) match {
                case Some(tg) =>
                  val (_, strs: Seq[String]) = tg.closing(s, ctx)
                  strs
                case None => Seq.empty
              }
            case t: TextEvent =>
              val trimmed = t.text.trim
              if (!trimmed.isEmpty() && !trimmed.isBlank()) {
                // println(s"pushing [${trimmed}]")
                ctx.textStack.push(trimmed)
              }
              Seq.empty
            case _ => Seq.empty
          }
      })
      .map(ByteString(_))
  }
}
