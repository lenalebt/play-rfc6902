package de.lenabrueder.rfc6902.patchset

import play.api.libs.json._
import play.api.libs.functional.syntax._

import scala.util.{ Failure, Success, Try }

/**
 * Here, all operation mapping takes place. This is where access rights need to be checked in case they are relevant.
 */

object PathSplitOperation {
  def splitPath(path: String) = path.split("/").filterNot(_.isEmpty)
  def toJsPath(path: String) = splitPath(path).foldLeft(JsPath())(_ \ _)
}
import PathSplitOperation.toJsPath

abstract class JsPatchOperation {
  def apply(jsValue: JsValue): Try[JsValue]
}
object JsPatchOperation {
  def apply(jsOperation: JsValue): JsPatchOperation = {
    jsOperation \ "op" match {
      case JsDefined(JsString(operation)) => Try(operation match {
        case "test"    => jsOperation.as[JsPatchTestOp]
        case "remove"  => jsOperation.as[JsPatchRemoveOp]
        case "add"     => jsOperation.as[JsPatchAddOp]
        case "replace" => jsOperation.as[JsPatchReplaceOp]
        case "move"    => jsOperation.as[JsPatchMoveOp]
        case "copy"    => jsOperation.as[JsPatchCopyOp]
      }) match {
        case Success(op)                        => op
        case Failure(JsResultException(errors)) => throw new IllegalArgumentException(s"cannot construct patch operation for entry $jsOperation: ${errors.toString()}")
        case Failure(ex)                        => throw new IllegalArgumentException(s"cannot construct patch operation for entry $jsOperation: $ex")
      }
      case _: JsUndefined => throw new IllegalArgumentException(s"No operation defined for entry $jsOperation in patch set")
    }
  }
}

/**
 * operation "test"
 * @param path
 * @param value
 */
case class JsPatchTestOp(path: String,
                         value: JsValue)
    extends JsPatchOperation {
  def apply(jsValue: JsValue): Try[JsValue] =
    jsValue.transform(toJsPath(path).json.pick) match {
      case JsSuccess(pickedValue, _) => {
        if (pickedValue == value)
          Success(jsValue)
        else
          Failure(new IllegalStateException(s"given value $pickedValue was not equal to $value"))
      }
      case _: JsError => Failure(new IllegalStateException(s"value at path $path could not be found"))
    }
}
object JsPatchTestOp {
  implicit val jsPatchTestOpFormat: Format[JsPatchTestOp] = (
    (JsPath \ "path").format[String] and
    (JsPath \ "value").format[JsValue]
  )(JsPatchTestOp.apply, unlift(JsPatchTestOp.unapply))
}

/**
 * operation "remove"
 * @param path
 */
case class JsPatchRemoveOp(path: String)
    extends JsPatchOperation {
  def apply(jsValue: JsValue): Try[JsValue] = {
    jsValue.transform(toJsPath(path).json.prune)
  }
}
object JsPatchRemoveOp {
  implicit val jsPatchRemoveOpFormat: Format[JsPatchRemoveOp] =
    (JsPath \ "path").format[String].inmap( //special case for single-element objects!
      JsPatchRemoveOp(_),
      (op: JsPatchRemoveOp) => op.path
    )
}

/**
 * operation "add"
 * @param path
 * @param value
 */
case class JsPatchAddOp(path: String,
                        value: JsValue)
    extends JsPatchOperation {
  def apply(jsValue: JsValue): Try[JsValue] = {
    jsValue.transform(toJsPath(path).json.put(value)) //TODO: this is wrong and does a replacement and does not correctly handle arrays.
  }
}
object JsPatchAddOp {
  implicit val jsPatchAddOpFormat: Format[JsPatchAddOp] = (
    (JsPath \ "path").format[String] and
    (JsPath \ "value").format[JsValue]
  )(JsPatchAddOp.apply, unlift(JsPatchAddOp.unapply))
}

/**
 * operation "replace"
 * @param path
 * @param value
 */

case class JsPatchReplaceOp(path: String,
                            value: JsValue)
    extends JsPatchOperation {
  def apply(jsValue: JsValue): Try[JsValue] =
    jsValue.transform(toJsPath(path).json.put(value))
}
object JsPatchReplaceOp {
  implicit val jsPatchReplaceOpFormat: Format[JsPatchReplaceOp] = (
    (JsPath \ "path").format[String] and
    (JsPath \ "value").format[JsValue]
  )(JsPatchReplaceOp.apply, unlift(JsPatchReplaceOp.unapply))
}

/**
 * operation "move"
 * @param pathFrom
 * @param pathTo
 * @param value
 */
case class JsPatchMoveOp(pathFrom: String,
                         pathTo: String,
                         value: JsValue)
    extends JsPatchOperation {
  def apply(jsValue: JsValue): Try[JsValue] =
    jsResult2Try(jsValue.transform(toJsPath(pathTo).json.copyFrom(toJsPath(pathFrom).json.pick))).flatMap(
      JsPatchRemoveOp(pathFrom)(_)
    )
  //TODO: check if this works correctly
}
object JsPatchMoveOp {
  implicit val jsPatchMoveOpFormat: Format[JsPatchMoveOp] = (
    (JsPath \ "from").format[String] and
    (JsPath \ "path").format[String] and
    (JsPath \ "value").format[JsValue]
  )(JsPatchMoveOp.apply, unlift(JsPatchMoveOp.unapply))
}

/**
 * operation "copy"
 * @param pathFrom
 * @param pathTo
 * @param value
 */
case class JsPatchCopyOp(pathFrom: String,
                         pathTo: String,
                         value: JsValue)
    extends JsPatchOperation {
  def apply(jsValue: JsValue): Try[JsValue] =
    jsValue.transform(toJsPath(pathTo).json.copyFrom(toJsPath(pathFrom).json.pick))
}
object JsPatchCopyOp {
  implicit val jsPatchCopyOpFormat: Format[JsPatchCopyOp] = (
    (JsPath \ "from").format[String] and
    (JsPath \ "path").format[String] and
    (JsPath \ "value").format[JsValue]
  )(JsPatchCopyOp.apply, unlift(JsPatchCopyOp.unapply))
}
