# Play-RFC6902
This is an implementation of [RFC6902](https://tools.ietf.org/html/rfc6902) for use
in [Play](https://www.playframework.com/) applications without the need for additional wrappers.

To use it in your project, simply include it in your `build.sbt`:

    libraryDependencies += "de.lenabrueder" %% "play-rfc6902" % "0.1"

# Examples

    val json = Json.parse("""{"a":"b", "b":{"c":"d"}, "c":1}""")
    JsPatch(Json.parse("""[{"op":"remove", "path":"/b"}]""")) match {
        case Right(patch) => patch(json) match {
            case Right(patchedJson) => Logger.debug(s"patched json: $patchedJson")
            case Left((patch, errors)) => Logger.debug(s"patch application failed, errors: $errors")
        }
        case Left((patch, errors)) => Logger.debug(s"patch parsing failed, errors: $errors")
    }

Will output

    patched json: {"a":"b","c":1}

For more examples, have a look at the [test cases](https://github.com/lenalebt/play-rfc6902/blob/master/src/test/scala/de/lenabrueder/rfc6902/JsonPatchSpec.scala).

Build with activator/sbt. There are test case failures at the moment for some edge cases, the test cases are already 
written to comply to the RFC6902, even if the code does not work this way currently.

## Plan for release 1.0
Release 1.0 will be [RFC6902](https://tools.ietf.org/html/rfc6902) and
[RFC6901](https://tools.ietf.org/html/rfc6901) compliant and be able to apply patches.