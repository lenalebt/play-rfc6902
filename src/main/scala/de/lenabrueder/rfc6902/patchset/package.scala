package de.lenabrueder.rfc6902

import play.api.libs.json._

import scala.language.implicitConversions
import scala.util.{ Failure, Success, Try }

package object patchset {
  implicit def jsResult2Try(jsResult: JsResult[JsObject]): Try[JsValue] = jsResult match {
    case JsSuccess(success, _) => Success(success)
    case JsError(errors)       => Failure(new IllegalArgumentException(errors.toString()))
  }
}
