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
  private case class Ctx(
      var tagContext: Stack[String] = Stack.empty,
      var textStack: Stack[String] = Stack.empty,
      var isHeadSection: Boolean = true
  )
  def connectToXmlConverter[A](
      source: Source[ByteString, A]
  ): Source[ByteString, A] = {
    source
      .via(XmlParsing.parser)
      .statefulMapConcat(() => {
        // state
        val ctx = Ctx()
        def possibleNewLine() = {
          if (ctx.isHeadSection) {
            ctx.isHeadSection = false
            ""
          } else {
            "\n\n"
          }
        }
        val safePop: () => String =
          () =>
            if (ctx.textStack.size == 0) ""
            else {
              val popped = ctx.textStack.pop()
              if (popped == " " || popped.isBlank) {
                ""
              } else {
                popped
              }
            }
        var sectionDepth: Int = 1 // ## から始めたい

        // agg
        ev =>
          ev match {
            case s: StartElement =>
              s.localName match {
                case "entry" =>
                  ctx.tagContext.push(s.localName)
                  Seq() // should be root
                case "meta" =>
                  // TODO: treat meta
                  ctx.tagContext.push(s.localName)
                  Seq()
                case "sec" =>
                  ctx.tagContext.push(s.localName)
                  sectionDepth += 1
                  Seq(
                    s"${safePop()}${possibleNewLine()}${"#"
                        .repeat(sectionDepth)} ${s.attributes.get("title").get}\n"
                  )
                case "codeblock" =>
                  ctx.tagContext.push(s.localName)
                  val lang = s.attributes.get("lang").getOrElse("")
                  val popped = safePop()
                  Seq(
                    s"${popped}${possibleNewLine()}```${lang}\n"
                  )
                case "para" =>
                  ctx.tagContext.push(s.localName)
                  Seq(s"${safePop()}\n\n")
                case "li" =>
                  ctx.tagContext.push(s.localName)
                  Seq("- ")
                case "ul" =>
                  ctx.tagContext.push(s.localName)
                  Seq(s"${safePop()}\n\n")
                case "code" =>
                  ctx.tagContext.push(s.localName)
                  Seq(s"${safePop()} ")
                case "em" =>
                  ctx.tagContext.push(s.localName)
                  Seq(s"${safePop()} ")
                case otherwise =>
                  println(s"*** Unknown element: ${s.localName}")
                  ctx.tagContext.push(s.localName)
                  Seq(safePop())
              }
            case s: EndElement =>
              val t = ctx.tagContext.pop()
              // println(s"*** lasting text: ${textBuffer.size}")
              t match {
                case "code"      => Seq(s"`${safePop()}`")
                case "em"        => Seq(s" **${safePop()}** ")
                case "codeblock" => Seq(s"${safePop()}\n```")
                // TODO: treat ol
                case "li" => Seq(s"${safePop()}\n")
                case "ul" => Seq("\n")
                case "sec" =>
                  sectionDepth -= 1
                  Seq(s"${safePop()}")
                case "entry"   => Seq(s"${safePop()}\n") // EOF
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
