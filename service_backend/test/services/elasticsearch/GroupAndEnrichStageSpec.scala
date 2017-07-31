package services.elasticsearch

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, MustMatchers, WordSpec}

class GroupAndEnrichStageSpec
    extends WordSpec
    with BeforeAndAfterAll
    with ScalaFutures
    with MustMatchers {

  implicit val as  = ActorSystem("BulkQueryGraphSpec")
  implicit val mat = ActorMaterializer()

  "BulkQueryGraph" must {

    case class In(i: Int)
    case class Out(i: Int, s: Option[String])

    def group(i: In) = i.i.toString.toCharArray.map(c => c.toString.toInt).toSet

    def transform(f: Set[Int]) =
      Map(1 -> "one", 2 -> "two", 3 -> "tree").filterKeys(f.contains).toSeq.toSet

    def resource(i: In, f: Set[(Int, String)]) =
      Out(i.i, f.find(_._1 == i.i % 10).map(_._2))

    "group items and transform when the stream ends" in {
      val limit      = 2
      var count: Int = 0
      val countAndTransform =
        (f: Set[Int]) => {
          count = count + 1
          transform(f)
        }
      val stage =
        new GroupAndEnrichStage[In, Out, Int, (Int, String)](
          group,
          countAndTransform,
          resource,
          limit
        )

      val source = Source(1 to 5).map(In)
      val res    = source.via(stage).runWith(Sink.seq).futureValue

      count mustBe 3
      res must have size 5
    }

    "group items and transform when the stream ends on the limit" in {
      val limit      = 2
      var count: Int = 0
      val countAndTransformCount =
        (f: Set[Int]) => {
          count = count + 1
          transform(f)
        }

      val stage =
        new GroupAndEnrichStage[In, Out, Int, (Int, String)](
          group,
          countAndTransformCount,
          resource,
          limit
        )

      val res = Source(1 to 4).map(In).via(stage).runWith(Sink.seq).futureValue

      count mustBe 2
      res must have size 4
    }

    "group, transform and reduce an item on the stream" in {
      val limit = 2
      val stage =
        new GroupAndEnrichStage[In, Out, Int, (Int, String)](
          group,
          transform,
          resource,
          limit
        )

      val res = Source.single(1).map(In).via(stage).runWith(Sink.headOption).futureValue

      res mustBe Some(Out(1, Some("one")))
    }
  }

  override def afterAll() = {
    as.terminate()
  }

}
