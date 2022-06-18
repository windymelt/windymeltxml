package windymelt

import akka.stream.alpakka.xml.StartElement
import akka.stream.alpakka.xml.EndElement

trait Tag {
  def opening(
      elem: StartElement,
      ctx: Converter.Ctx
  ): (Converter.Ctx, Seq[String]) = (ctx, Seq())
  def closing(
      elem: EndElement,
      ctx: Converter.Ctx
  ): (Converter.Ctx, Seq[String])
}
