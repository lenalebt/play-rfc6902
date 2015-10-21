package de.lenabrueder.rfc6902.patchset

import play.api.libs.json._
import play.api.libs.functional.syntax._

import scala.util.{ Failure, Right }

/**
 * Here, all operation mapping takes place. This is where access rights need to be checked in case they are relevant.
 */

object PathSplitOperation {
  def splitPath(path: String): Array[String] = path.split("/").filterNot(_.isEmpty)

  def toJsPath(path: String): JsPath = splitPath(path).foldLeft(JsPath())(_ \ _)
}

import PathSplitOperation.toJsPath

/**
 * A json patch operation which can be constructed from its JSON representation.
 */
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

  implicit val jsPatchOpWrites: Writes[JsPatchOperation] = new Writes[JsPatchOperation] {
    override def writes(o: JsPatchOperation): JsValue = o match {
      case o @ JsPatchTestOp(path, value)      => Json.toJson(o)(JsPatchTestOp.jsPatchTestOpWrites)
      case o @ JsPatchRemoveOp(path)           => Json.toJson(o)(JsPatchRemoveOp.jsPatchRemoveOpWrites)
      case o @ JsPatchAddOp(path, value)       => Json.toJson(o)(JsPatchAddOp.jsPatchAddOpWrites)
      case o @ JsPatchReplaceOp(path, value)   => Json.toJson(o)(JsPatchReplaceOp.jsPatchReplaceOpWrites)
      case o @ JsPatchMoveOp(pathFrom, pathTo) => Json.toJson(o)(JsPatchMoveOp.jsPatchMoveOpWrites)
      case o @ JsPatchCopyOp(pathFrom, pathTo) => Json.toJson(o)(JsPatchCopyOp.jsPatchCopyOpWrites)
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
  implicit val jsPatchTestOpReads: Reads[JsPatchTestOp] = (
    (JsPath \ "path").read[String] and
    (JsPath \ "value").read[JsValue]
  )(JsPatchTestOp.apply _)

  implicit val jsPatchTestOpWrites: Writes[JsPatchTestOp] = new Writes[JsPatchTestOp] {
    override def writes(o: JsPatchTestOp): JsValue = Json.obj(
      "op" -> "test",
      "path" -> o.path,
      "value" -> o.value
    )
  }
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
  implicit val jsPatchRemoveOpReads: Reads[JsPatchRemoveOp] = new Reads[JsPatchRemoveOp] {
    override def reads(json: JsValue): JsResult[JsPatchRemoveOp] =
      (json \ "path").validate[String].map(JsPatchRemoveOp(_))
  }

  implicit val jsPatchRemoveOpWrites: Writes[JsPatchRemoveOp] = new Writes[JsPatchRemoveOp] {
    override def writes(o: JsPatchRemoveOp): JsValue = Json.obj(
      "op" -> "remove",
      "path" -> o.path
    )
  }
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
  implicit val jsPatchAddOpReads: Reads[JsPatchAddOp] = (
    (JsPath \ "path").read[String] and
    (JsPath \ "value").read[JsValue]
  )(JsPatchAddOp.apply _)

  implicit val jsPatchAddOpWrites: Writes[JsPatchAddOp] = new Writes[JsPatchAddOp] {
    override def writes(o: JsPatchAddOp): JsValue = Json.obj(
      "op" -> "add",
      "path" -> o.path,
      "value" -> o.value
    )
  }
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
  implicit val jsPatchReplaceOpReads: Reads[JsPatchReplaceOp] = (
    (JsPath \ "path").read[String] and
    (JsPath \ "value").read[JsValue]
  )(JsPatchReplaceOp.apply _)

  implicit val jsPatchReplaceOpWrites: Writes[JsPatchReplaceOp] = new Writes[JsPatchReplaceOp] {
    override def writes(o: JsPatchReplaceOp): JsValue = Json.obj(
      "op" -> "replace",
      "path" -> o.path,
      "value" -> o.value
    )
  }
}

/**
 * operation "move"
 * @param pathFrom
 * @param pathTo
 */
case class JsPatchMoveOp(pathFrom: String,
                         pathTo: String)
    extends JsPatchOperation {
  def apply(jsValue: JsValue): Either[PatchApplicationError, JsValue] = {
    val jsPathTo: JsPath = toJsPath(pathTo)
    val jsPathFrom: JsPath = toJsPath(pathFrom)
    JsPatchCopyOp(pathFrom, pathTo)(jsValue) match {
      case Right(copiedJs)                    => JsPatchRemoveOp(pathFrom)(copiedJs)
      case Left(CopyFailed(pathFrom, pathTo)) => Left(MoveFailed(pathFrom, pathTo))
      case Left(errors)                       => Left(errors)
    }

    //jsValue.transform(jsPathTo.json.copyFrom(jsPathFrom.json.pick)) match{
    //  case JsSuccess(copiedJs,_) =>JsPatchRemoveOp(pathFrom)(copiedJs)
    //  case _:JsError => Left(MoveFailed(value, jsPathTo,jsPathFrom))
    //}
  }

  //TODO: check if this works correctly
}

object JsPatchMoveOp {
  implicit val jsPatchMoveOpReads: Reads[JsPatchMoveOp] = (
    (JsPath \ "from").read[String] and
    (JsPath \ "path").read[String]
  )(JsPatchMoveOp.apply _)

  implicit val jsPatchMoveOpWrites: Writes[JsPatchMoveOp] = new Writes[JsPatchMoveOp] {
    override def writes(o: JsPatchMoveOp): JsValue = Json.obj(
      "op" -> "move",
      "from" -> o.pathFrom,
      "path" -> o.pathTo
    )
  }
}

/**
 * operation "copy"
 * @param pathFrom
 * @param pathTo
 */
case class JsPatchCopyOp(pathFrom: String,
                         pathTo: String)
    extends JsPatchOperation {
  def apply(jsValue: JsValue): Either[PatchApplicationError, JsValue] = {
    val jsPathFrom: JsPath = toJsPath(pathFrom)
    val jsPathTo: JsPath = toJsPath(pathTo)
    jsValue.transform(jsPathTo.json.copyFrom(jsPathFrom.json.pick)) match {
      case JsSuccess(copiedJs, _) => Right(copiedJs)
      case _: JsError             => Left(CopyFailed(jsPathFrom, jsPathTo))
    }
  }
}

object JsPatchCopyOp {
  implicit val jsPatchCopyOpReads: Reads[JsPatchCopyOp] = (
    (JsPath \ "from").read[String] and
    (JsPath \ "path").read[String]
  )(JsPatchCopyOp.apply _)

  implicit val jsPatchCopyOpWrites: Writes[JsPatchCopyOp] = new Writes[JsPatchCopyOp] {
    override def writes(o: JsPatchCopyOp): JsValue = Json.obj(
      "op" -> "copy",
      "from" -> o.pathFrom,
      "path" -> o.pathTo
    )
  }
}
