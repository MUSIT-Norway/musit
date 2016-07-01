package no.uio.musit.microservice.storageAdmin.utils

object TupleImplicits {
  implicit def flatten1[A, B, C, D, E, F, G, H, I, J, K, L](t: ((A, B, C, D, E, F, G, H, I, J, K), L)): (A, B, C, D, E, F, G, H, I, J, K, L) = (t._1._1, t._1._2, t._1._3, t._1._4, t._1._5, t._1._6, t._1._7, t._1._8, t._1._9, t._1._10, t._1._11, t._2)
  implicit def flatten2[A, B, C, D, E, F, G, H, I, J, K, L, M](t: ((A, B, C, D, E, F, G, H, I, J, K), (L, M))): (A, B, C, D, E, F, G, H, I, J, K, L, M) = (t._1._1, t._1._2, t._1._3, t._1._4, t._1._5, t._1._6, t._1._7, t._1._8, t._1._9, t._1._10, t._1._11, t._2._1, t._2._2)
  implicit def flatten8[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S](t: ((A, B, C, D, E, F, G, H, I, J, K), (L, M, N, O, P, Q, R, S))): (A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S) = (t._1._1, t._1._2, t._1._3, t._1._4, t._1._5, t._1._6, t._1._7, t._1._8, t._1._9, t._1._10, t._1._11, t._2._1, t._2._2, t._2._3, t._2._4, t._2._5, t._2._6, t._2._7, t._2._8)
}
