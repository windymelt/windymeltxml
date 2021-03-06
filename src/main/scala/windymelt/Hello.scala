package windymelt

import akka.stream.scaladsl.Source
import akka.stream.scaladsl.FileIO
import java.nio.file.Path
import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.util.ByteString
import akka.stream.alpakka.xml.scaladsl.XmlParsing
import akka.stream.alpakka.xml.StartElement

import akka.stream.alpakka.xml.EndElement
import akka.stream.alpakka.xml.TextEvent
import akka.stream.scaladsl.Sink
import scala.concurrent.Await
import scala.concurrent.duration._

object Hello extends App {
  val system = ActorSystem()
  implicit val mat = Materializer(system)
  val file: Path = Path.of("file.xml")
  val outfile: Path = Path.of("file.md")
  val sink = FileIO.toPath(outfile)
  val grph = Converter
    .connectToXmlConverter(
      FileIO
        .fromPath(file)
        .map(ByteString(_))
    )
    .to(sink)
  val run = grph.run()
  import scala.language.postfixOps
  Await.ready(run, 10 seconds)
  Thread.sleep(1000)
  run.foreach(anyway => system.terminate())(mat.executionContext)
}
