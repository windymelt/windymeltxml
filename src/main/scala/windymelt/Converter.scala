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
              s.localName match {
                case "entry" =>
                  val (_, strs: Seq[String]) = tag.Entry.opening(s, ctx)
                  strs
                case "meta" =>
                  val (_, strs: Seq[String]) = tag.Meta.opening(s, ctx)
                  strs
                case "sec" =>
                  val (_, strs: Seq[String]) = tag.Sec.opening(s, ctx)
                  strs
                case "codeblock" =>
                  val (_, strs: Seq[String]) = tag.Codeblock.opening(s, ctx)
                  strs
                case "para" =>
                  val (_, strs: Seq[String]) = tag.Para.opening(s, ctx)
                  strs
                case "li" =>
                  val (_, strs: Seq[String]) = tag.Li.opening(s, ctx)
                  strs
                case "ul" =>
                  val (_, strs: Seq[String]) = tag.Ul.opening(s, ctx)
                  strs
                case "code" =>
                  val (_, strs: Seq[String]) = tag.Code.opening(s, ctx)
                  strs
                case "em" =>
                  val (_, strs: Seq[String]) = tag.Em.opening(s, ctx)
                  strs
                case otherwise =>
                  println(s"*** Unknown element: ${s.localName}")
                  Seq(ctx.safePop())
              }
            case s: EndElement =>
              val t = s.localName
              // println(s"*** lasting text: ${textBuffer.size}")
              t match {
                case "code" =>
                  val (_, strs: Seq[String]) = tag.Code.closing(s, ctx)
                  strs
                case "em" =>
                  val (_, strs: Seq[String]) = tag.Em.closing(s, ctx)
                  strs
                case "codeblock" =>
                  val (_, strs: Seq[String]) = tag.Codeblock.closing(s, ctx)
                  strs
                // TODO: treat ol
                case "li" =>
                  val (_, strs: Seq[String]) = tag.Li.closing(s, ctx)
                  strs
                case "ul" =>
                  val (_, strs: Seq[String]) = tag.Ul.closing(s, ctx)
                  strs
                case "sec" =>
                  val (_, strs: Seq[String]) = tag.Sec.closing(s, ctx)
                  strs
                case "entry" =>
                  val (_, strs: Seq[String]) = tag.Entry.closing(s, ctx)
                  strs
                case "para" =>
                  val (_, strs: Seq[String]) = tag.Para.closing(s, ctx)
                  strs
                case otherwise => Seq.empty // do nothing
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
