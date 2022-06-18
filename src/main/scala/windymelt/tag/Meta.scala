package windymelt
package tag

import akka.stream.alpakka.xml.EndElement

object Meta extends Tag {
  def closing(
      elem: EndElement,
      ctx: Converter.Ctx
  ): (Converter.Ctx, Seq[String]) = ctx -> Seq()
}
