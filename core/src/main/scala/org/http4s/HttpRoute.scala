package org.http4s

import cats.Applicative
import cats.data.{Kleisli, OptionT}
import cats.effect.Sync
import cats.implicits._

/** Functions for creating [[HttpRoutes]]. */
object HttpRoutes {
  /** Lifts a function into [[HttpRoutes]].
    * 
    * @tparam F the effect of the [[HttpRoutes]]
    * @param run the function to lift
    * @return an [[HttpRoutes]] that wraps `run`
    */
  def apply[F[_]](run: Request[F] => OptionT[F, Response[F]]): HttpRoutes[F] =
    Kleisli(run)

  /** Lifts an effectful [[Response]] into [[HttpRoutes]]. 
    * 
    * @tparam F the effect of the [[HttpRoutes]]
    * @param fr the effectful [[Response]] to lift
    * @return an [[HttpRoutes]] that always returns `fr`
    */
  def liftF[F[_]](fr: OptionT[F, Response[F]]): HttpRoutes[F] =
    Kleisli.liftF(fr)

  /** Lifts a [[Response]] into [[HttpRoutes]]. 
    * 
    * @tparam F the base effect of the [[HttpRoutes]]
    * @param r the [[Response]] to lift
    * @return an [[HttpRoutes]] that always returns `r` in effect `OptionT[F, ?]`
    */
  def pure[F[_]](r: Response[F])(implicit FO: Applicative[OptionT[F, ?]]): HttpRoutes[F] =
    Kleisli.pure(r)

  /** Transforms [[HttpRoutes]] on its input.
    * 
    * @tparam F the base effect of the [[HttpRoutes]]
    * @param f a function to apply to the [[Request]]
    * @param fa the [[HttpRoutes]] to transform
    * @return [[HttpRoutes]] whose input is transformed by `f` before
    * being applied to `fa`
    */
  def local[F[_]](f: Request[F] => Request[F])(fa: HttpRoutes[F]): HttpRoutes[F] =
    Kleisli.local[OptionT[F, ?], Response[F], Request[F]](f)(fa)

  /** Lifts a partial function into an [[HttpRoutes]].  The application of the
    * partial function is delayed in `F` to permit more efficient combination
    * of routes via `SemigroupK`.
    * 
    * @tparam F the base effect of the [[HttpRoutes]]
    * @param pfthe partial function to lift
    * @return [[HttpRoutes]] that returns some [[Response]] in an `OptionT[F, ?]`
    * wherever `pf` is defined, an `OptionT.none` wherever it is not
    */
  def of[F[_]](pf: PartialFunction[Request[F], F[Response[F]]])(implicit F: Sync[F]): HttpRoutes[F] =
    Kleisli(req => OptionT(F.delay(pf.lift(req).sequence).flatten))
}
