package de.lenabrueder.rfc6902

import de.lenabrueder.UnitSpec
import de.lenabrueder.rfc6902.patchset._
import play.api.libs.json._

/**
 * Here are all test cases that are needed for RFC6902 compliance.
 *
 * Test cases that do not have anything to do with RFC6902 compliance are found elsewhere.
 */
class JsonPatchRFC6902Spec extends UnitSpec {
  val json = Json.parse("""{"a":"b", "b":{"c":"d"}, "c":1}""")

  "JsPatch" should {
    "do nothing with an empty patch set" in {
      val patch = JsPatch(Json.parse("""[]"""))
      patch shouldBe 'right
      patch.right.get(json) should equal(Right(json))
    }

    "support the \"remove\" functionality" in {
      val patch = JsPatch(Json.parse("""[{"op":"remove", "path":"/b"}]"""))
      patch shouldBe 'right
      patch.right.get(json) should equal(Right(Json.parse("""{"a":"b", "c":1}""")))
    }

    "support the \"test\" functionality" in {
      val patch = JsPatch(Json.parse("""[{"op":"test", "path":"/a", "value":"b"}]"""))
      patch shouldBe 'right
      patch.right.get(json) should equal(Right(json))
    }

    "support the \"replace\" functionality" in {
      val patch = JsPatch(Json.parse("""[{"op":"replace", "path":"/a", "value":"g"}]"""))
      patch shouldBe 'right
      patch.right.get(json) should equal(Right(Json.parse("""{"a":"g", "b":{"c":"d"}, "c":1}""")))
    }
    "\"replace\" should only replace the value it points at" in {
      val patch = JsPatch(Json.parse("""[{"op":"replace", "path":"/a/b", "value":"g"}]"""))
      patch shouldBe 'right
      patch.right.get(Json.parse("""{"a":{"b":1,"c":2}}""")) should equal(Right(Json.parse("""{"a":{"b":"g","c":2}}""")))
    }
    "support the \"add\" functionality" in {
      val patch = JsPatch(Json.parse("""[{"op":"add", "path":"/d", "value":"g"}]"""))
      patch shouldBe 'right
      patch.right.get(json) should equal(Right(Json.parse("""{"a":"b", "b":{"c":"d"}, "c":1, "d":"g"}""")))
    }
    "\"add\" should only replace the value it points at" in {
      val patch = JsPatch(Json.parse("""[{"op":"add", "path":"/a/d", "value":"g"}]"""))
      patch shouldBe 'right
      patch.right.get(Json.parse("""{"a":{"b":1,"c":2}}""")) should equal(Right(Json.parse("""{"a":{"b":1,"c":2,"d":"g"}}""")))
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
      patch.right.get(json) should equal(Right(Json.parse("""{"a":"b", "c":1}""")))
    }

    "support filters" in {
      val patch = JsPatch(Json.parse("""[{"op":"remove", "path":"/b"},{"op":"remove", "path":"/a"}]"""))
      patch shouldBe 'right
      patch.right.get(json, {
        case JsPatchRemoveOp(path) => (path.startsWith("/a").unary_!, None)
        case _                     => (true, None)
      }) should equal(Left(Json.parse("""{"a":"b", "c":1}"""), Seq(FilterMismatch(JsPatchRemoveOp("/a")))))
    }
  }

  "JsPatch.add" should {
    "support adding array elements" in pending
    "support adding an array element at the beginning" in pending
    "support adding an array element somewhere in the middle" in pending
    "support adding an array element at their end" in pending
    "support adding numbers" in {
      val patch = JsPatch(Json.parse("""[{"op":"add", "path":"/e", "value":271}]"""))
      patch shouldBe 'right
      patch.right.get(json) should equal(Right(Json.parse("""{"a":"b", "b":{"c":"d"}, "c":1, "e":271}""")))
    }
    "support adding strings" in {
      val patch = JsPatch(Json.parse("""[{"op":"add", "path":"/f", "value":"pink fluffy unicorns dancing on rainbows"}]"""))
      patch shouldBe 'right
      patch.right.get(json) should equal(Right(Json.parse("""{"a":"b", "b":{"c":"d"}, "c":1, "f":"pink fluffy unicorns dancing on rainbows"}""")))
    }
    "support adding nulls" in {
      val patch = JsPatch(Json.parse("""[{"op":"add", "path":"/g", "value":null}]"""))
      patch shouldBe 'right
      patch.right.get(json) should equal(Right(Json.parse("""{"a":"b", "b":{"c":"d"}, "c":1, "g":null}""")))
    }
    "support adding bools" in {
      val patch = JsPatch(Json.parse("""[{"op":"add", "path":"/g", "value":false}]"""))
      patch shouldBe 'right
      patch.right.get(json) should equal(Right(Json.parse("""{"a":"b", "b":{"c":"d"}, "c":1, "g":false}""")))
    }
    "replace values that are already there" in {
      val patch = JsPatch(Json.parse("""[{"op":"add", "path":"/a", "value":false}]"""))
      patch shouldBe 'right
      patch.right.get(json) should equal(Right(Json.parse("""{"a":false, "b":{"c":"d"}, "c":1}""")))
    }
    "add values that are not already there" in {
      val patch = JsPatch(Json.parse("""[{"op":"add", "path":"/k", "value":false}]"""))
      patch shouldBe 'right
      patch.right.get(json) should equal(Right(Json.parse("""{"a":"b", "b":{"c":"d"}, "c":1, "k":false}""")))
    }

    //from the docs for the next two test cases:
    /*
     * However, the object itself or an array containing it does need to
     * exist, and it remains an error for that not to be the case.  For
     * example, an "add" with a target location of "/a/b" starting with this
     * document:
     *
     * { "a": { "foo": 1 } }
     *
     * is not an error, because "a" exists, and "b" will be added to its
     * value.  It is an error in this document:
     *
     * { "q": { "bar": 2 } }
     *
     * because "a" does not exist.
    */
    "add to objects that are not already existing" in {
      val patch = JsPatch(Json.parse("""[{"op":"add", "path":"/b/d", "value":false}]"""))
      patch shouldBe 'right
      patch.right.get(json) should equal(Right(Json.parse("""{"a":"b", "b":{"c":"d", "d":false}, "c":1}""")))
    }
    "not add if the paths parent does not already exist" in {
      val patch = JsPatch(Json.parse("""[{"op":"add", "path":"/d/e", "value":false}]"""))
      patch shouldBe 'right
      patch.right.get(json) should equal(Left(json, Seq(AddFailed(JsBoolean(false), JsPath \ "d" \ "e"))))
    }
  }
  "JsPatch.remove" should {
    "not be successful when the path to be removed does not exist" in {
      val patch = JsPatch(Json.parse("""[{"op":"remove", "path":"/d"}]"""))
      patch shouldBe 'right
      patch.right.get(json) shouldBe 'left
    }
    "remove a path that exists" in {
      val patch = JsPatch(Json.parse("""[{"op":"remove", "path":"/b/c"}]"""))
      patch shouldBe 'right
      patch.right.get(json) should equal(Right(Json.parse("""{"a":"b","b":{},"c":1}""")))
    }
    "remove an element from the beginning of an array" in pending
    "remove an element from the middle of an array" in pending
    "remove an element from the end of an array" in pending
  }
  "JsPatch.replace" should {
    "fail if the target does not exist" in {
      val patch = JsPatch(Json.parse("""[{"op":"replace", "path":"/d", "value":"g"}]"""))
      patch shouldBe 'right
      patch.right.get(json) should equal(Left((json, Seq(ReplaceFailedPathDidNotExist(JsPath \ "d")))))
    }
    """behave exactly as "first remove, then add" """ in pending
  }
  "JsPatch.move" should {
    "not be successful if the source location does not exist" in {
      val patch = JsPatch(Json.parse("""[{"op":"move", "from":"/d", "path":"/g"}]"""))
      patch shouldBe 'right
      patch.right.get(json) should equal(Left((json, Seq(MoveFailed(JsPath \ "d", JsPath \ "g")))))
    }
    """behave exactly as "first remove on source, then add at target location" """ in pending
    "fail if the target location is below the source location" in pending
  }
  "JsPatch.copy" should {
    "fail if the source location does not exist" in {
      val patch = JsPatch(Json.parse("""[{"op":"copy", "from":"/d", "path":"/g"}]"""))
      patch shouldBe 'right
      patch.right.get(json) should equal(Left((json, Seq(CopyFailed(JsPath \ "d", JsPath \ "g")))))
    }
    """behave exactly like "add the value located at source at the target" """ in pending
  }
  "JsPatch.test" should {
    "fail if the json type at the location and in the operation do not match" in {
      val patch1 = JsPatch(Json.parse("""[{"op":"test", "path":"/a", "value":1}]"""))
      val patch2 = JsPatch(Json.parse("""[{"op":"test", "path":"/b", "value":1}]"""))
      val patch3 = JsPatch(Json.parse("""[{"op":"test", "path":"/c", "value":"1"}]"""))
      val patch4 = JsPatch(Json.parse("""[{"op":"test", "path":"/a", "value":true}]"""))
      val patch5 = JsPatch(Json.parse("""[{"op":"test", "path":"/b", "value":true}]"""))
      val patch6 = JsPatch(Json.parse("""[{"op":"test", "path":"/c", "value":true}]"""))

      for (patch <- Seq(patch1, patch2, patch3, patch4, patch5, patch6)) {
        patch match {
          case Right(patch) => patch(json) shouldBe 'left
          case Left(patch)  => fail(s"the above patches should all be correct, especially $patch")
        }
      }
    }
    "fail if the values are not equal" in {
      val patch1 = JsPatch(Json.parse("""[{"op":"test", "path":"/a", "value":"a"}]"""))
      val patch2 = JsPatch(Json.parse("""[{"op":"test", "path":"/b", "value":{"c":"e"}}]"""))
      val patch3 = JsPatch(Json.parse("""[{"op":"test", "path":"/c", "value":10}]"""))

      for (patch <- Seq(patch1, patch2, patch3)) {
        patch match {
          case Right(patch) => patch(json) shouldBe 'left
          case Left(patch)  => fail(s"the above patches should all be correct, especially $patch")
        }
      }
    }
    "tests strings correctly for equality" in {
      val patch1 = JsPatch(Json.parse("""[{"op":"test", "path":"/a", "value":"b"}]"""))
      val patch2 = JsPatch(Json.parse("""[{"op":"test", "path":"/b/c", "value":"d"}]"""))

      for (patch <- Seq(patch1, patch2)) {
        patch match {
          case Right(patch) =>
            patch(json) match {
              case Right(patched) => patched should equal(json)
              case Left(patched)  => fail(s"test should not change the json, the result was $patched")
            }

          case Left(patch) => fail(s"the above patches should all be correct, especially $patch")
        }
      }
    }
    "tests ints and ints correctly for equality" in {
      val json = Json.parse("""{"a":1, "b":{"c":2}, "c":3}""")
      val patch1 = JsPatch(Json.parse("""[{"op":"test", "path":"/a", "value":1}]"""))
      val patch2 = JsPatch(Json.parse("""[{"op":"test", "path":"/b/c", "value":2}]"""))
      val patch3 = JsPatch(Json.parse("""[{"op":"test", "path":"/c", "value":3}]"""))

      for (patch <- Seq(patch1, patch2, patch3)) {
        patch match {
          case Right(patch) =>
            patch(json) match {
              case Right(patched) => patched should equal(json)
              case Left(patched)  => fail(s"test should not change the json, the result was $patched")
            }

          case Left(patch) => fail(s"the above patches should all be correct, especially $patch")
        }
      }
    }
    "tests floats and floats correctly for equality" in {
      val json = Json.parse("""{"a":1.0, "b":{"c":2.0}, "c":3.0}""")
      val patch1 = JsPatch(Json.parse("""[{"op":"test", "path":"/a", "value":1.0}]"""))
      val patch2 = JsPatch(Json.parse("""[{"op":"test", "path":"/b/c", "value":2.0}]"""))
      val patch3 = JsPatch(Json.parse("""[{"op":"test", "path":"/c", "value":3.0}]"""))

      for (patch <- Seq(patch1, patch2, patch3)) {
        patch match {
          case Right(patch) =>
            patch(json) match {
              case Right(patched) => patched should equal(json)
              case Left(patched)  => fail(s"test should not change the json, the result was $patched")
            }

          case Left(patch) => fail(s"the above patches should all be correct, especially $patch")
        }
      }
    }
    "tests floats and ints correctly for equality" in pending
    "tests ints and floats correctly for equality" in pending
    "tests arrays correctly for equality" in pending
    "does not consider arrays to be equal when they have the same elements, but at different positions" in pending
    "tests json objects correctly for equality" in {
      //is the case when they contain the same number of elements, and the elements test equal according to normal
      //equality test rules. ordering does not matter.
      pending
    }
    "tests json literals true, false, null for equality correctly" in pending
  }
}
