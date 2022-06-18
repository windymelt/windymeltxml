package windymelt

import akka.stream.alpakka.xml.TextEvent

object Text {
  def apply(
      t: TextEvent,
      ctx: Converter.Ctx
  ): (Converter.Ctx, Seq[String]) = {
    val trimmed = t.text.trim
    if (!trimmed.isEmpty() && !trimmed.isBlank()) {
      // println(s"pushing [${trimmed}]")
      ctx.textStack.push(trimmed)
    }
    ctx -> Seq.empty
  }
}
