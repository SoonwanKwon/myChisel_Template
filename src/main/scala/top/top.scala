package top

import chisel3._
import chisel3.util._
import freechips.rocketchip.diplomacy._
import adder_config._
import core_complex._
import freenmp._
import adder_config.SmallConfig
import java.io.{File, FileWriter}
import freechips.rocketchip.util.ElaborationArtefacts
import org.chipsalliance.cde.config.{Config, Parameters}


object TopMain extends App {

  val opts = if (args.isEmpty)  Map.empty[String, String]
              else  args.grouped(2).collect { case Array(k,v) => k -> v}.toMap

  val configName: String = opts.getOrElse("--config", "empty").toLowerCase()
  val designName: String = opts.getOrElse("--design", "empty").toLowerCase()

  val targetDir   = s"./build/${designName}"
  val firrtlOpts  = Array("--target-dir", targetDir, "--no-run-firrtl", "--chisel-output-file", designName)

  val firtoolOpts = Array("--split-verilog", 
          "--disable-all-randomization",
          "-O=debug",
      )

  implicit  lazy val config:  Config = configName match {
    case "empty" => new Config(Parameters.empty)
    case "adder_config" => new SmallConfig()
    case "core_complex" => new Bus64BitConfig()
    case _ => {
      new Config(Parameters.empty)
    }
  }

  println(s"design is ${designName}")

  val soc : LazyModule = designName match {
    case "adder_config" => {
      LazyModule(new AdderTestHarness()(new SmallConfig))
    }
    case "core_complex"=> {
      LazyModule(new CoreComplexTestHarness()(config))
    }
	//case "nmp" => {
	  //LazyModule(new XBAR()(new MyBaseConfig))
	//}
    case _ => {
      throw new RuntimeException(s"Invalid design name: ${designName}")
    }
  }

  Generator.execute(firrtlOpts, soc.module, firtoolOpts, designName, targetDir)

  ElaborationArtefacts.files.foreach { case(extension, contents)=>
    val f = new File(s"./${targetDir}", "TestHarness." + extension)
    val fw = new FileWriter(f)
    fw.write(contents())
    fw.close
  }
  println("Complete")


}
