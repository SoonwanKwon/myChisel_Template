// See LICENSE.SiFive for license details.

package core_complex

import java.io.{File, FileWriter}

import chisel3._
import circt.stage._
import org.chipsalliance.cde.config.{Config, Parameters,Field}
//import freechips.rocketchip.unittest.Generator.{generateAnno, generateArtefacts, generateFirrtl, generateTestSuiteMakefrags}
import freechips.rocketchip.util.ElaborationArtefacts;

object Generator {
    final def main(args: Array[String]) {


	val targetDir = new File("test_run_dir")

 
	 val args: Array[String] = Array(
	        "--target",
	        "systemverilog",
	        "--target-dir",
	        targetDir.toString
	)

        // val p = (new Default2Config).toInstance
        val p = (new Default1Config).toInstance

		def top = new CoreComplexTestHarness()(p)

		val generator = Seq(chisel3.stage.ChiselGeneratorAnnotation(() => top))
		(new ChiselStage).execute(args, generator :+ FirtoolOption("--disable-annotation-unknown") )

        ElaborationArtefacts.files.foreach { case (extension, contents) =>
            val f = new File(".", "TestHarness." + extension)
            val fw = new FileWriter(f)
            fw.write(contents())
            fw.close
        }
        // generateArtefacts
    }
}
