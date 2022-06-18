package windymelt
package tag

import akka.stream.alpakka.xml.StartElement
import akka.stream.alpakka.xml.EndElement

object Li extends Tag {
  override def opening(
      elem: StartElement,
      ctx: Converter.Ctx
  ): (Converter.Ctx, Seq[String]) = ctx -> Seq("- ")
  def closing(
      elem: EndElement,
      ctx: Converter.Ctx
  ): (Converter.Ctx, Seq[String]) = ctx -> Seq(s"${ctx.safePop()}\n")
}
