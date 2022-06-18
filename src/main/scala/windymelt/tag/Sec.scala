package windymelt
package tag

import akka.stream.alpakka.xml.StartElement
import akka.stream.alpakka.xml.EndElement

object Sec extends Tag {
  override def opening(
      elem: StartElement,
      ctx: Converter.Ctx
  ): (Converter.Ctx, Seq[String]) = {
    ctx.sectionDepth += 1
    ctx -> Seq(
      s"${ctx.possibleNewLine()}${"#"
          .repeat(ctx.sectionDepth)} ${elem.attributes.get("title").get}\n"
    )
  }
  def closing(
      elem: EndElement,
      ctx: Converter.Ctx
  ): (Converter.Ctx, Seq[String]) = {
    ctx.sectionDepth -= 1
    ctx -> Seq(s"${ctx.safePop()}")
  }
}
