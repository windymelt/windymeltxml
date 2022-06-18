package windymelt
package tag

import akka.stream.alpakka.xml.EndElement
import akka.stream.alpakka.xml.StartElement

object Codeblock extends Tag {
  override def opening(
      elem: StartElement,
      ctx: Converter.Ctx
  ): (Converter.Ctx, Seq[String]) = {
    val lang = elem.attributes.get("lang").getOrElse("")
    val popped = ctx.safePop()
    ctx -> Seq(
      s"${popped}${ctx.possibleNewLine()}```${lang}\n"
    )
  }
  def closing(
      elem: EndElement,
      ctx: Converter.Ctx
  ): (Converter.Ctx, Seq[String]) = {
    ctx -> Seq(s"${ctx.safePop()}\n```\n")
  }
}
