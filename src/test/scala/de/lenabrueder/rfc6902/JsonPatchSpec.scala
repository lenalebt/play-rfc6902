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

  "JsPatch.add" should {
    "support adding array elements" in pending
    "support adding an array element at the beginning" in pending
    "support adding an array element somewhere in the middle" in pending
    "support adding an array element at ther end" in pending
    "support adding numbers" in pending
    "support adding strings" in pending
    "support adding nulls" in pending
    "support adding bools" in pending
    "replace values that are already there" in pending
    "add values that are not already there" in pending
    "add to objects that are not already existing" in pending
    "not add if the paths parent does not already exist" in {
      //from the docs:
      /*
      However, the object itself or an array containing it does need to
   exist, and it remains an error for that not to be the case.  For
   example, an "add" with a target location of "/a/b" starting with this
   document:

   { "a": { "foo": 1 } }

   is not an error, because "a" exists, and "b" will be added to its
   value.  It is an error in this document:

   { "q": { "bar": 2 } }

   because "a" does not exist.
      */
      pending
    }
  }
  "JsPatch.remove" should {
    "not be successful when the path to be removed does not exist" in pending
    "remove a path that exists" in pending
    "remove an element from the beginning of an array" in pending
    "remove an element from the middle of an array" in pending
    "remove an element from the end of an array" in pending
  }
  "JsPatch.replace" should {
    "fail if the target does not exist" in pending
    """behave exactly as "first remove, then add" """ in pending
  }
  "JsPatch.move" should {
    "not be successful if the source location does not exist" in pending
    """behave exactly as "first remove on source, then add at target location" """ in pending
    "fail if the target location is below the source location" in pending
  }
  "JsPatch.copy" should {
    "fail if the source location does not exist" in pending
    """behave exactly like "add the value located at source at the target" """ in pending
  }
  "JsPatch.test" should {
    "fail if the json type at the location and in the operation do not match" in pending
    "fail if the values are not equal" in pending
    "tests strings correctly for equality" in pending
    "tests ints and ints correctly for equality" in pending
    "tests floats and floats correctly for equaliy" in pending
    "tests floats and ints correctly for equality" in pending
    "tests ints and floats correctly for equality" in pending
    "tests arrays correctly for equality" in pending
    "does not consider arrays to be equal when they have the same elements, but at different positions" in pending
    "tests json objects correctly for equality" in pending //is the case when they contain the same number of elements, and the elements test equal according to normal equality test rules. ordering does not matter.
    "tests json literals true, false, null for equality correctly" in pending
  }
}
