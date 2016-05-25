/*
 *   MUSIT is a cooperation between the university museums of Norway.
 *   Copyright (C) 2016  MUSIT Norway, part of www.uio.no (University of Oslo)
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation; either version 2 of the License,
 *   or any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License along
 *   with this program; if not, write to the Free Software Foundation, Inc.,
 *   51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package no.uio.musit.microservices.common.extensions

/**
  * Created by jstabel on 5/25/16.
  */


import play.api.libs.functional.Functor

object FunctorExtensions {


  trait Functor[A, F[_] <: Functor[_, F]] {

    def fmap[B](f: A => B): F[B]

  }
  //  def innerMap[F1[_]: Functor, A, F2[A]: Functor, B](functorFunctor: F1[F2[A]], f: A=>B): F1[F2[B]] = { functorFunctor.fmap{_.fmap(f)} }


  def mapTest[A, B, F[A] <: Functor[A,F]](functor: F[A], f: A=>B) = { functor.fmap(f)}
  def innerMap[A, B, F1[A] <: Functor[A,F1], F2[B] <: Functor[B, F2]](functorFunctor: F1[F2[A]], f: A=>B): F1[F2[B]] = { functorFunctor.fmap{_.fmap(f)} }

  /*
  implicit class FunctorFunctorExtensionsImp[F1[_]: Functor, A, F2[A]: Functor](val functorFunctor: F1[F2[A]]) extends AnyVal {
  def innerMap[B](f: A=>B): F1[F2[B]] = { functorFunctor.fmap{_.fmap(f)} }
  }*/
}
