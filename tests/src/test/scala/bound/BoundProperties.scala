package bound

import org.scalacheck.Properties
import org.scalacheck.Prop
import org.scalacheck.Prop._

abstract class BoundProperties(name: String) extends Properties(name){

  def test(name:String)(f: => Prop) = property(name) = secure {
    try f catch { case e: java.lang.Throwable  => e.printStackTrace(System.err); throw e }
  }
}
