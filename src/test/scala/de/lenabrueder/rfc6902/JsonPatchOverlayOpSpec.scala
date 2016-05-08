package de.lenabrueder.rfc6902

import de.lenabrueder.UnitSpec
import play.api.libs.json._

class JsonPatchOverlayOpSpec extends UnitSpec {
  val overlayOp = "overlay"
  val json = Json.parse("""{"a":"b", "b":{"c":"d"}, "c":1}""")

  "JsPatchOverlayOp" should {
    "correctly add flat elements to a sub-path" in {
      val patch = JsPatch(Json.parse(s"""{"op":"$overlayOp", "path":"/b", "value": {"rainbow": "unicorn"}}"""))
      patch shouldBe 'right
      patch.right.get(json) should equal(Right(Json.parse("""{"a":"b", "b":{"c":"d", "rainbow":"unicorn"}, "c":1}""")))
    }

    "correctly add complex elements to a sub-path" in {
      val patch = JsPatch(Json.parse(s"""{"op":"$overlayOp", "path":"/b", "value": {"rainbow": {"complex":"unicorn"}}}"""))
      patch shouldBe 'right
      patch.right.get(json) should equal(Right(Json.parse("""{"a":"b", "b":{"c":"d", "rainbow": {"complex":"unicorn"}}, "c":1}""")))
    }

    "correctly create a path from scratch that does not exist in the original JSON" in {
      val patch = JsPatch(Json.parse(s"""{"op":"$overlayOp", "path":"/z/y/x", "value": {"rainbow": {"complex":"unicorn"}}}"""))
      patch shouldBe 'right
      patch.right.get(json) should equal(Right(Json.parse("""{"a":"b", "b":{"c":"d"}, "c":1,"z":{"y":{"x":{"rainbow": {"complex":"unicorn"}}}}}""")))
    }

    "not care at all about the original type of a path when it should overlay at that path" in {
      //for example the case when trying to write something to path "/c" in the above example
      val patch = JsPatch(Json.parse(s"""{"op":"$overlayOp", "path":"/c", "value": {"rainbow": "unicorn"}}"""))
      patch shouldBe 'right
      patch.right.get(json) should equal(Right(Json.parse("""{"a":"b", "b":{"c":"d"}, "c":{"rainbow": "unicorn"}}""")))
    }

    "not care at all about the original type of a path when it should overlay at a deeper point from that path" in {
      //for example the case when trying to write something to path "/c" in the above example
      val patch = JsPatch(Json.parse(s"""{"op":"$overlayOp", "path":"/c/d", "value": {"rainbow": "unicorn"}}"""))
      patch shouldBe 'right
      patch.right.get(json) should equal(Right(Json.parse("""{"a":"b", "b":{"c":"d"}, "c":{"d": {"rainbow": "unicorn"}}}""")))
    }
  }
}
