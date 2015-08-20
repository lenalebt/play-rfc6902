package de.lenabrueder.rfc6902

import de.lenabrueder.rfc6902.patchset.JsPatchOperation
import play.api.libs.json._

import scala.util.{ Failure, Success, Try }

case class JsPatch(patchSet: Seq[JsPatchOperation]) {
  def apply(jsValue: JsValue,filter: JsPatchOperation => Boolean={_:JsPatchOperation=>true}): Try[JsValue] = {
    patchSet.foldLeft(Try(jsValue)) { (jsResult, op) =>
      jsResult flatMap {
        if (filter(op))
          op(_)
        else
          Try(_)
      }
    }
  }
}

object JsPatch {
  /**
   *
   * @param patchSet May be either an array of patches or a single patch
   * @return a JsPatch that can be applied to any given JsValue to patch it.
   */
  def apply(patchSet: JsValue): JsPatch = {
    patchSet match {
      case patchArray: JsArray => JsPatch(patchArray.as[Seq[JsValue]].map(JsPatchOperation(_)))
      case singlePatch         => JsPatch(Seq(JsPatchOperation(singlePatch)))
    }
  }
}