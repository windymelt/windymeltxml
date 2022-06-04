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
  def connectToXmlConverter[A](
      source: Source[ByteString, A]
  ): Source[ByteString, A] = {
    source
      .via(XmlParsing.parser)
      .statefulMapConcat(() => {
        // state
        var context: Stack[String] = Stack.empty
        var textBuffer: Stack[String] = Stack.empty
        var isHeadSection = true
        def possibleNewLine() = {
          if (isHeadSection) {
            isHeadSection = false
            ""
          } else {
            "\n\n"
          }
        }
        val safePop: () => String =
          () =>
            if (textBuffer.size == 0) ""
            else {
              val popped = textBuffer.pop()
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
                  context.push(s.localName)
                  Seq() // should be root
                case "meta" =>
                  // TODO: treat meta
                  context.push(s.localName)
                  Seq()
                case "sec" =>
                  context.push(s.localName)
                  sectionDepth += 1
                  Seq(
                    s"${safePop()}${possibleNewLine()}${"#"
                        .repeat(sectionDepth)} ${s.attributes.get("title").get}\n"
                  )
                case "codeblock" =>
                  context.push(s.localName)
                  val lang = s.attributes.get("lang").getOrElse("")
                  val popped = safePop()
                  Seq(
                    s"${popped}${possibleNewLine()}```${lang}\n"
                  )
                case "para" =>
                  context.push(s.localName)
                  Seq(s"${safePop()}\n\n")
                case "li" =>
                  context.push(s.localName)
                  Seq("- ")
                case "ul" =>
                  context.push(s.localName)
                  Seq(s"${safePop()}\n\n")
                case "code" =>
                  context.push(s.localName)
                  Seq(s"${safePop()} ")
                case "em" =>
                  context.push(s.localName)
                  Seq(s"${safePop()} ")
                case otherwise =>
                  println(s"*** Unknown element: ${s.localName}")
                  context.push(s.localName)
                  Seq(safePop())
              }
            case s: EndElement =>
              val t = context.pop()
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
                textBuffer.push(trimmed)
              }
              Seq.empty
            case _ => Seq.empty
          }
      })
      .map(ByteString(_))
  }
}
