package de.lenabrueder.rfc6902.patchset

import play.api.libs.json.JsValue

/**
 * Created by lena on 20.08.15.
 */
sealed trait PatchConstructionError
case class IllegalOperation(jsValue: JsValue) extends PatchConstructionError
case class IllegalParameters(jsValue: JsValue) extends PatchConstructionError
case class IllegalPatch(jsValue: JsValue) extends PatchConstructionError
