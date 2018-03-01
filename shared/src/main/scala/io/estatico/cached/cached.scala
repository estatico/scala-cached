package io.estatico.cached

import scala.annotation.StaticAnnotation
import scala.reflect.macros.whitebox

class cached extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro CachedMacros.cached
}

