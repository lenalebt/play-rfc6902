package de.lenabrueder.rfc6902.patchset

import play.api.libs.json.{ JsPath, JsValue }

sealed trait PatchApplicationError

trait TestFailedError extends PatchApplicationError
case class TestFailedValuesUnequal(pickedValue: JsValue, testValue: JsValue, path: JsPath) extends TestFailedError
case class TestFailedPathNotFound(path: JsPath) extends TestFailedError

trait RemoveFailedError extends PatchApplicationError
case class RemoveFailedPathNotFound(path: JsPath) extends RemoveFailedError
case class RemoveFailed(path: JsPath) extends RemoveFailedError

trait AddFailedError extends PatchApplicationError
case class AddFailed(value: JsValue, path: JsPath) extends AddFailedError

trait ReplaceFailedError extends PatchApplicationError
case class ReplaceFailed(value: JsValue, path: JsPath) extends ReplaceFailedError

trait MoveFailedError extends PatchApplicationError
case class MoveFailed(value: JsValue, pathTo: JsPath, pathFrom: JsPath) extends MoveFailedError

trait CopyFailedError extends PatchApplicationError
case class CopyFailed(value: JsValue, pathTo: JsPath, pathFrom: JsPath) extends CopyFailedError

trait FilterMismatchError extends PatchApplicationError
case class FilterMismatch(jsPatchOperation: JsPatchOperation) extends FilterMismatchError