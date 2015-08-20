package de.lenabrueder.rfc6902

import de.lenabrueder.rfc6902.patchset._
import org.scalatest.{ FlatSpec, Matchers, MustMatchers, WordSpec }
import play.api.libs.json._

import scala.util.{ Failure, Success }

class JsonPatchSpec extends WordSpec with Matchers {
  val json = Json.parse("""{"a":"b", "b":{"c":"d"}}""")

  "JsPatch" should {
    "do nothing with an empty patch set" in {
      val patch = JsPatch(Json.parse("""[]"""))
      patch shouldBe 'right
      patch.right.get(json) should equal(Right(json))
    }

    "support the \"remove\" functionality" in {
      val patch = JsPatch(Json.parse("""[{"op":"remove", "path":"/b"}]"""))
      patch shouldBe 'right
      patch.right.get(json) should equal(Right(Json.parse("""{"a":"b"}""")))
    }

    "support the \"test\" functionality" in {
      val patch = JsPatch(Json.parse("""[{"op":"test", "path":"/a", "value":"b"}]"""))
      patch shouldBe 'right
      patch.right.get(json) should equal(Right(json))
    }

    "fail when no \"value\" is given for operation \"replace\" in the patch" in {
      val patch = JsPatch(Json.parse("""[{"op":"replace", "path":"/b"}]"""))
      patch shouldBe 'left
      patch.left.get shouldBe ((JsPatch(Seq.empty), Seq(IllegalParameters(Json.parse("""{"op":"replace", "path":"/b"}""")))))
    }

    "fail when no \"op\" is given in the patch" in {
      val patch = JsPatch(Json.parse("""[{"path":"/b"}]"""))
      patch shouldBe 'left
      patch.left.get shouldBe ((JsPatch(Seq.empty), Seq(IllegalPatch(Json.parse("""{"path":"/b"}""")))))
    }

    "support single patches" in {
      val patch = JsPatch(Json.parse("""{"op":"remove", "path":"/b"}"""))
      patch shouldBe 'right
      patch.right.get(json) should equal(Right(Json.parse("""{"a":"b"}""")))
    }

    "support filters" in {
      val patch = JsPatch(Json.parse("""[{"op":"remove", "path":"/b"},{"op":"remove", "path":"/a"}]"""))
      patch shouldBe 'right
      patch.right.get(json, {
        case JsPatchRemoveOp(path) => path.startsWith("/a").unary_!
        case _                     => true
      }) should equal(Left(Json.parse("""{"a":"b"}"""), Seq(FilterMismatch(JsPatchRemoveOp("/a")))))
    }
  }
}
