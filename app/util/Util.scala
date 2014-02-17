package util

import scala.concurrent.{Promise, Future}
import com.twitter.util.{Throw, Return}

object Util {
  def twitterToScalaFuture[A](twitterFuture: com.twitter.util.Future[A]): Future[A] = {
    val promise = Promise[A]()
    twitterFuture respond {
      case Return(a) => promise success a
      case Throw(e) => promise failure e
    }
    promise.future
  }
}
