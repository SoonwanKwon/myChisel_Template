package adder_config

import org.chipsalliance.cde.config.{Config, Parameters,Field}
import chisel3._
import freechips.rocketchip.diplomacy.{LazyModule, LazyModuleImp, NexusNode, RenderedEdge, SimpleNodeImp, SinkNode, SourceNode, ValName}
import freechips.rocketchip.util.ElaborationArtefacts

case object NumOperands extends Field[Int]
case object BitWidth extends Field[Int]

/** top-level connector */
class AdderTop()(implicit p: Parameters) extends LazyModule {
  val numOperands = p(NumOperands)
  val bitWidth = p(BitWidth)

  val adder = LazyModule(new Adder)
  // 8 will be the downward-traveling widths from our drivers
  val drivers = Seq.fill(numOperands) { LazyModule(new AdderDriver(width = bitWidth, numOutputs = 2)) }
  // 4 will be the upward-traveling width from our monitor
  val monitor = LazyModule(new AdderMonitor(width = bitWidth, numOperands = numOperands))

  // create edges via binding operators between nodes in order to define a complete graph
  drivers.foreach{ driver => adder.node := driver.node }

  drivers.zip(monitor.nodeSeq).foreach { case (driver, monitorNode) => monitorNode := driver.node }
  monitor.nodeSum := adder.node

  lazy val module = new Impl
  class Impl extends LazyModuleImp(this) {
    val io = IO(new Bundle {
      val finished = Output(Bool())
    })
    when(monitor.module.io.error) {
      printf("something went wrong")
    }
    io.finished := monitor.module.io.error
  }

  override lazy val desiredName = "AdderTestHarness"
}

class AdderTestHarness()(implicit p: Parameters) extends LazyModule {
  lazy val module = new Impl
  class Impl extends LazyModuleImp(this) {
    val io = IO(new Bundle {
      val success = Output(Bool())
    })
  
    val ldut = LazyModule(new AdderTop)
    val dut = Module(ldut.module)
    io.success := dut.io.finished
  
    ElaborationArtefacts.add("graphml", ldut.graphML)
  }
}


