package de.lenabrueder.rfc6902

import de.lenabrueder.rfc6902.patchset.{ FilterMismatch, PatchConstructionError, PatchApplicationError, JsPatchOperation }
import play.api.libs.json._

import scala.util.{ Failure, Success, Try }

/**
 * Class holding a JsPatch with its operations.
 */
case class JsPatch(patchSet: Seq[JsPatchOperation]) {
  def apply[T](
    jsValue: JsValue,
    filter:  JsPatchOperation => (Boolean, Option[T]) = { _: JsPatchOperation => (true, None) }
  ): Either[(JsValue, Seq[PatchApplicationError]), JsValue] = {
    val initValue: (JsValue, Seq[PatchApplicationError]) = (jsValue, Seq.empty)
    val (result, errors) = patchSet.foldLeft(initValue) { (resultAndErrors, op) =>
      val (updatedJs, updatedErrors) = resultAndErrors
      val (filterResult, optReason) = filter(op)
      if (filterResult) {
        op(updatedJs) match {
          case Right(newJsResult) => (newJsResult, updatedErrors)
          case Left(newErrors)    => (updatedJs, updatedErrors :+ newErrors)
        }
      } else
        (updatedJs, updatedErrors :+ FilterMismatch(op, optReason))
    }

    errors match {
      case Nil => Right(result)
      case _   => Left(result, errors)
    }
  }
}

object JsPatch {
  /**
   *
   * @param patchSet May be either an array of patches or a single patch
   * @return a JsPatch that can be applied to any given JsValue to patch it.
   */
  def apply(patchSet: JsValue): Either[(JsPatch, Seq[PatchConstructionError]), JsPatch] = {
    patchSet match {
      case patchArray: JsArray =>
        val ops = for (jsOp <- patchArray.as[Seq[JsValue]]) yield { JsPatchOperation(jsOp) }
        val patch = JsPatch(ops.collect { case Right(op) => op })
        ops.collect { case Left(error) => error } match {
          case Nil    => Right(patch)
          case errors => Left((patch, errors))
        }

      case singlePatch =>
        JsPatchOperation(singlePatch) match {
          case Right(op)   => Right(JsPatch(Seq(op)))
          case Left(error) => Left((JsPatch(Seq.empty), Seq(error)))
        }

    }
  }
}
