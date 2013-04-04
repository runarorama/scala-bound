package bound

import org.scalacheck.Properties
import org.scalacheck.Prop
import org.scalacheck.Prop._

abstract class BoundProperties(name: String) extends Properties(name){

  def trying(f: => Prop) = secure {
    try f catch { case e: Throwable  => e.printStackTrace; throw e }
  }

  def test(name:String)(f: => Prop) = property(name) = trying(f)
}
