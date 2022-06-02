package windymelt

import akka.stream.scaladsl.Source
import akka.stream.scaladsl.FileIO
import java.nio.file.Path
import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.util.ByteString
import akka.stream.alpakka.xml.scaladsl.XmlParsing
import akka.stream.alpakka.xml.StartElement
import scala.collection.mutable.Stack
import akka.stream.alpakka.xml.EndElement
import akka.stream.alpakka.xml.TextEvent
import akka.stream.scaladsl.Sink

object Hello extends App {
  val system = ActorSystem()
  implicit val mat = Materializer(system)
  val file: Path = Path.of("file.xml")
  val outfile: Path = Path.of("file.md")
  val sink = FileIO.toPath(outfile)
  val grph = FileIO
    .fromPath(file)
    .map(ByteString(_))
    .via(XmlParsing.parser)
    .statefulMapConcat(() => {
      // state
      var context: Stack[String] = Stack.empty
      var textBuffer: Stack[String] = Stack.empty
      val safePop: () => String =
        () => if (textBuffer.size == 0) "" else textBuffer.pop()
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
                  s"${"#".repeat(sectionDepth)} ${s.attributes.get("title").get}\n"
                )
              case "codeblock" =>
                context.push(s.localName)
                val lang = s.attributes.get("lang").getOrElse("")
                Seq(
                  s"${safePop()}\n\n```${lang}\n"
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
              case "codeblock" => Seq(s"${safePop()}\n```\n")
              // TODO: treat ol
              case "li" => Seq(s"${safePop()}\n")
              case "ul" => Seq("\n")
              case "sec" =>
                sectionDepth -= 1
                Seq(s"${safePop()}\n\n")
              case otherwise => Seq.empty // do nothing
            }
          case t: TextEvent =>
            // do nothing
            textBuffer.push(t.text.trim)
            Seq.empty
          case _ => Seq.empty
        }
    })
    .map(ByteString(_))
    .to(sink)
  val run = grph.run()
  run.foreach(anyway => system.terminate())(mat.executionContext)
}
