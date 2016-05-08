package de.lenabrueder.rfc6902

import de.lenabrueder.UnitSpec
import play.api.libs.json.{ JsValue, Json }

/**
 * TODO: description
 */
class JsPatchConvenienceFunctionalitySpec extends UnitSpec {
  "A Seq[JsPatchOp]" should {
    "be convertible to JSON" in {
      val jsPatch: JsValue =
        Json.parse("""[{"op":"add", "path":"/b/d", "value":false},{"op":"test", "path":"/a", "value":1}]""")
      val patch = JsPatch(jsPatch)
      patch shouldBe 'right
      Json.toJson(patch.right.get.patchSet) shouldBe jsPatch
    }
  }
}
