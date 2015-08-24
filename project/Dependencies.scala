import sbt._, Keys._

object Dependencies {

  object Version {
    val scalaz = "7.1.3"
    val scalaprops = "0.1.13"
    val scalatest = "2.2.5"
  }

  val scalaz = "org.scalaz" %% "scalaz-core" % Version.scalaz
  val scalatest = "org.scalatest" %% "scalatest" % Version.scalatest
}