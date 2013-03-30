package bound

import f0._
import Writers._
import scalaz.\/

object BoundSerialization {

  def varW[B,F1,F,F2](wb: Writer[B,F1], wf: Writer[F,F2]): Writer[\/[B,F],S2[F1,F2]] =
    s2W(wb,wf)((a,b) => _.fold(a,b))

  trait ForallW[F[+_]] { def apply[A](wa: Writer[A, DynamicF]): Writer[F[A], DynamicF] }

  def scopeW[B, BF, F[+_], V, VF](
    pb: Writer[B, BF],
    pf: ForallW[F],
    pv: Writer[V, VF]
  ): Writer[Scope[B, F, V], DynamicF] =
    pf(varW(pb.erase, pf(pv.erase)).erase).cmap((s: Scope[B, F, V]) => s.unscope)
}
