package windymelt
package tag

import akka.stream.alpakka.xml.EndElement
import akka.stream.alpakka.xml.StartElement

object Ul extends Tag {
  override def opening(
      elem: StartElement,
      ctx: Converter.Ctx
  ): (Converter.Ctx, Seq[String]) = ctx -> Seq(s"${ctx.safePop()}\n\n")
  def closing(
      elem: EndElement,
      ctx: Converter.Ctx
  ): (Converter.Ctx, Seq[String]) = ctx -> Seq("\n")
}
