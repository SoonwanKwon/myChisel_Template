// See LICENSE.SiFive for license details.

package core_complex

import chisel3._
import org.chipsalliance.cde.config.{Config, Parameters,Field}
import freechips.rocketchip.diplomacy.{LazyModule, LazyModuleImp}
import freechips.rocketchip.util.ElaborationArtefacts

class CoreComplexTestHarness()(implicit p: Parameters) extends LazyModule {
  lazy val module = new Impl
  class Impl extends LazyModuleImp(this){ 
    val io = IO(new Bundle {
      val success = Output(Bool())
    })
  
    val ldut = LazyModule(new core_complex())
    val dut = Module(ldut.module)
  
    io.success := dut.io.finished
  
    ElaborationArtefacts.add("graphml", ldut.graphML)
  }
}
