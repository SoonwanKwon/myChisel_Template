// import Mill dependency
import mill._
import mill.define.Sources
import mill.modules.Util
import mill.scalalib.scalafmt.ScalafmtModule
import mill.scalalib.TestModule.ScalaTest
import mill.scalalib._
import scala.util.Properties.{envOrElse}
import $file.`rocket-chip`.common
import $file.`rocket-chip`.cde.common
import $file.`rocket-chip`.hardfloat.build
import $file.`constellation`.common

// support BSP
import mill.bsp._

val defaultScalaVersion = "2.13.10"

def defaultVersions(chiselVersion: String) = chiselVersion match {
  case "chisel" => Map(
    "chisel"        -> ivy"org.chipsalliance::chisel:6.2.0",
    "chisel-plugin" -> ivy"org.chipsalliance:::chisel-plugin:6.2.0",
    "chiseltest"    -> ivy"edu.berkeley.cs::chiseltest:5.0.2"
  )
  case "chisel3" => Map(
    "chisel"        -> ivy"edu.berkeley.cs::chisel3:3.6.0",
    "chisel-plugin" -> ivy"edu.berkeley.cs:::chisel3-plugin:3.6.0",
    "chiseltest"    -> ivy"edu.berkeley.cs::chiseltest:0.6.2"
  )
}

trait HasChisel extends SbtModule with Cross.Module[String] {
  def chiselModule: Option[ScalaModule] = None

  def chiselPluginJar: T[Option[PathRef]] = None

  def chiselIvy: Option[Dep] = Some(defaultVersions(crossValue)("chisel"))

  def chiselPluginIvy: Option[Dep] = Some(defaultVersions(crossValue)("chisel-plugin"))

  override def scalaVersion = defaultScalaVersion

  override def scalacOptions = super.scalacOptions() ++
    Agg("-language:reflectiveCalls", "-Ymacro-annotations", "-Ytasty-reader")

  override def ivyDeps = super.ivyDeps() ++ Agg(chiselIvy.get)

  override def scalacPluginIvyDeps = super.scalacPluginIvyDeps() ++ Agg(chiselPluginIvy.get)
}

object rocketchip extends Cross[RocketChip]("chisel", "chisel3")

trait RocketChip
  extends millbuild.`rocket-chip`.common.RocketChipModule
    with HasChisel {
  def scalaVersion: T[String] = T(defaultScalaVersion)

  override def millSourcePath = os.pwd / "rocket-chip"

  def macrosModule = macros

  def hardfloatModule = hardfloat(crossValue)

  def cdeModule = cde

  def mainargsIvy = ivy"com.lihaoyi::mainargs:0.5.4"

  def json4sJacksonIvy = ivy"org.json4s::json4s-jackson:4.0.6"

  object macros extends Macros

  trait Macros
    extends millbuild.`rocket-chip`.common.MacrosModule
      with SbtModule {

    def scalaVersion: T[String] = T(defaultScalaVersion)

    def scalaReflectIvy = ivy"org.scala-lang:scala-reflect:${defaultScalaVersion}"
  }

  object hardfloat extends Cross[Hardfloat](crossValue)

  trait Hardfloat
    extends millbuild.`rocket-chip`.hardfloat.common.HardfloatModule with HasChisel {

    def scalaVersion: T[String] = T(defaultScalaVersion)

    override def millSourcePath = os.pwd / "rocket-chip" / "hardfloat" / "hardfloat"

  }

  object cde extends CDE

  trait CDE extends millbuild.`rocket-chip`.cde.common.CDEModule with ScalaModule {

    def scalaVersion: T[String] = T(defaultScalaVersion)

    override def millSourcePath = os.pwd / "rocket-chip" / "cde" / "cde"
  }
}

// trait for MyModule
trait HasRocketModule extends ScalaModule {
	def rocketModule: ScalaModule

	override def moduleDeps = super.moduleDeps ++ Seq(
		rocketModule,
	)
}

////// constellation
object	constellation extends Cross[CONSTELLATION]("chisel","chisel3")

trait	CONSTELLATION extends millbuild.`constellation`.common.constellationModule with HasChisel with ScalaModule {
	
	def	scalaVersion: T[String] = T(defaultScalaVersion)
	override def millSourcePath = os.pwd / "constellation"
	override def rocketModule = rocketchip(crossValue)
}






//////

object myModule extends Cross[MyModule]("chisel","chisel3")
trait MyModule extends HasChisel {

	val debug = envOrElse("DEBUG", "NO")

	override def millSourcePath = os.pwd

	def rocketModule = rocketchip(crossValue)
	def constellationModule = constellation(crossValue)

	override def moduleDeps = super.moduleDeps ++ Seq(
		rocketModule,
		constellationModule,
	)
	override def forkArgs = {
		if(debug == "ON")
			 Seq("-agentlib:jdwp=transport=dt_socket, server=y, address=8000, suspend=y")
		else
			 Seq("-Xmx8G", "-Xss256m")
	}

	override def sources = T.sources {
		super.sources() ++ Seq(PathRef(millSourcePath / "src" / "main"))
	}

	object test extends SbtModuleTests with TestModule.ScalaTest {
		override def sources = T.sources { 
			//println(PathRef(millSourcePath / "src" /"test"))
			super.sources() ++ Seq(PathRef(millSourcePath / "src" /"test"))
		}

 	  override def ivyDeps = super.ivyDeps() ++ Agg(
    	  defaultVersions(crossValue)("chiseltest")
      )

	}

}


