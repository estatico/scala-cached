package io.estatico.cached

import scala.reflect.macros.whitebox

@macrocompat.bundle
private[cached] class CachedMacros(val c: whitebox.Context) {

  import c.universe._

  def cached(annottees: Expr[Any]*): Tree = annottees match {
    case List(Expr(d@DefDef(_, _, _, _, _, _))) => run(d)
    case _ => fail(s"Only def methods can be cached")
  }

  private def fail(msg: String) = c.abort(c.enclosingPosition, msg)

  private def run(d: DefDef) = {
    if (d.vparamss.nonEmpty) fail(s"Cannot cache def methods with parameter lists")
    val nothings = d.tparams.map(t =>
      if (t.tparams.isEmpty) q"type ${t.name} = Nothing"
      else q"type ${t.name}[..${t.tparams}] = Nothing"
    )
    val cachedName = TermName(s"__cached__${d.name}")
    q"""
      ${d.mods} def ${d.name.toTermName}[..${d.tparams}]: ${d.tpt} = $cachedName.asInstanceOf[${d.tpt}]
      private val $cachedName = {
        ..$nothings

        { ${d.rhs} }: ${d.tpt}
      }
    """
  }
}

