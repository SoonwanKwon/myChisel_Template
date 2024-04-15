package adder_config
import circt.stage._
import java.io.{File, FileWriter}

import freechips.rocketchip.util.ElaborationArtefacts
import freechips.rocketchip.diplomacy.ValName

object Generator extends App {
    implicit val valName = ValName("testHarness")
	println("Generator executing")
	def top = new TestHarness()((new SmallConfig).toInstance)

	val generator = Seq(chisel3.stage.ChiselGeneratorAnnotation( () => top))
	(new ChiselStage).execute(args, generator :+ CIRCTTargetAnnotation(CIRCTTarget.Verilog))

    ElaborationArtefacts.files.foreach { case (extension, contents) =>
      val f = new File(".", "TestHarness." + extension)
      val fw = new FileWriter(f)
      fw.write(contents())
      fw.close
    }
}
