package de.lenabrueder.rfc6902.patchset

import play.api.libs.json._
import play.api.libs.functional.syntax._

import scala.util.{ Failure, Right }

/**
 * Here, all operation mapping takes place. This is where access rights need to be checked in case they are relevant.
 */

object PathSplitOperation {
  def splitPath(path: String) = path.split("/").filterNot(_.isEmpty)

  def toJsPath(path: String) = splitPath(path).foldLeft(JsPath())(_ \ _)
}

import PathSplitOperation.toJsPath

sealed abstract class JsPatchOperation {
  def apply(jsValue: JsValue): Either[PatchApplicationError, JsValue]
}

object JsPatchOperation {
  def apply(jsOperation: JsValue): Either[PatchConstructionError, JsPatchOperation] = {
    jsOperation \ "op" match {
      case JsDefined(JsString(operation)) => (operation match {
        case "test"    => Right(jsOperation.validate[JsPatchTestOp])
        case "remove"  => Right(jsOperation.validate[JsPatchRemoveOp])
        case "add"     => Right(jsOperation.validate[JsPatchAddOp])
        case "replace" => Right(jsOperation.validate[JsPatchReplaceOp])
        case "move"    => Right(jsOperation.validate[JsPatchMoveOp])
        case "copy"    => Right(jsOperation.validate[JsPatchCopyOp])
        case _         => Left(IllegalOperation(jsOperation))
      }) match {
        case Right(JsSuccess(op, _)) => Right(op)
        case Right(JsError(errors))  => Left(IllegalParameters(jsOperation))
        case Left(error)             => Left(error)
      }
      case JsDefined(_)   => Left(IllegalPatch(jsOperation))
      case _: JsUndefined => Left(IllegalPatch(jsOperation))
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
  def apply(jsValue: JsValue): Either[PatchApplicationError, JsValue] = {
    val jsPath = toJsPath(path)
    jsValue.transform(jsPath.json.pick) match {
      case JsSuccess(pickedValue, _) => {
        if (pickedValue == value)
          Right(jsValue)
        else
          Left(TestFailedValuesUnequal(pickedValue, testValue = value, jsPath))
      }
      case _: JsError => Left(TestFailedPathNotFound(jsPath))
    }
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
  def apply(jsValue: JsValue): Either[PatchApplicationError, JsValue] = {
    val jsPath: JsPath = toJsPath(path)
    jsValue.transform(jsPath.json.pick) match {
      case JsSuccess(pickedValue, _) => jsValue.transform(jsPath.json.prune) match {
        case JsSuccess(transformedJs, _) => Right(transformedJs)
        case _: JsError                  => Left(RemoveFailed(jsPath))
      }
      case _: JsError => Left(RemoveFailedPathNotFound(jsPath))
    }
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
  def apply(jsValue: JsValue): Either[PatchApplicationError, JsValue] = {
    val jsPath: JsPath = toJsPath(path)
    jsValue.transform(jsPath.json.put(value)) match {
      //TODO: this is wrong and does a replacement and does not correctly handle arrays.
      case JsSuccess(transformedJs, _) => Right(jsValue.as[JsObject] ++ transformedJs)
      case _: JsError                  => Left(AddFailed(value, jsPath))
    }
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
  def apply(jsValue: JsValue): Either[PatchApplicationError, JsValue] = {
    val jsPath: JsPath = toJsPath(path)
    jsValue.transform(jsPath.json.pick) match {
      case JsSuccess(pickedValue, _) =>
        jsValue.transform(jsPath.json.put(value)) match {
          case JsSuccess(transformedJs, _) => Right(jsValue.as[JsObject] ++ transformedJs)
          case _: JsError                  => Left(ReplaceFailed(value, jsPath))
        }
      case _: JsError => Left(ReplaceFailedPathDidNotExist(jsPath))
    }

  }
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
  def apply(jsValue: JsValue): Either[PatchApplicationError, JsValue] = {
    val jsPathTo: JsPath = toJsPath(pathTo)
    val jsPathFrom: JsPath = toJsPath(pathFrom)
    JsPatchCopyOp(pathFrom, pathTo, value)(jsValue) match {
      case Right(copiedJs) => JsPatchRemoveOp(pathFrom)(copiedJs)
      case Left(error)     => Left(error)
    }

    //jsValue.transform(jsPathTo.json.copyFrom(jsPathFrom.json.pick)) match{
    //  case JsSuccess(copiedJs,_) =>JsPatchRemoveOp(pathFrom)(copiedJs)
    //  case _:JsError => Left(MoveFailed(value, jsPathTo,jsPathFrom))
    //}
  }

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
  def apply(jsValue: JsValue): Either[PatchApplicationError, JsValue] = {
    val jsPathTo: JsPath = toJsPath(pathTo)
    val jsPathFrom: JsPath = toJsPath(pathFrom)
    jsValue.transform(jsPathTo.json.copyFrom(jsPathFrom.json.pick)) match {
      case JsSuccess(copiedJs, _) => Right(copiedJs)
      case _: JsError             => Left(CopyFailed(value, jsPathTo, jsPathFrom))
    }
  }
}

object JsPatchCopyOp {
  implicit val jsPatchCopyOpFormat: Format[JsPatchCopyOp] = (
    (JsPath \ "from").format[String] and
    (JsPath \ "path").format[String] and
    (JsPath \ "value").format[JsValue]
  )(JsPatchCopyOp.apply, unlift(JsPatchCopyOp.unapply))
}
