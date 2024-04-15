package core_complex

import chisel3._
import org.chipsalliance.cde.config.{Field, Parameters, _}
import freechips.rocketchip.tilelink._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.util.ElaborationArtefacts

case object BusWidthBytes extends Field[Int]
case object ConnectIfu extends Field[Boolean]
case object AddrSize extends Field[Int]

class core_complex(implicit p: Parameters) extends LazyModule {

  val xbar   = LazyModule(new TLXbar)
  val memory = LazyModule(new TLRAM(AddressSet(0x0, p(AddrSize)-1), beatBytes = p(BusWidthBytes)))
  //val memory = LazyModule(new TLRAM(AddressSet(0x0, p(AddrSize)-1), beatBytes = p(BusWidthBytes)))
  memory.node := TLFragmenter(p(BusWidthBytes), 256) := TLDelayer(0.1) := xbar.node

  val pusher1 = LazyModule(new TLPatternPusher("pat1", Seq(
    new WritePattern(0x100, 0x2, 0x012345678L),
    new WritePattern(0x500, 0x2, 0x0abcdef01L),
    new ReadExpectPattern(0x100, 0x2, 0x012345678L),
    new ReadExpectPattern(0x500, 0x2, 0x0abcdef01L)
  )))
  xbar.node := pusher1.node

  // val pusher2 = LazyModule(new TLPatternPusher("pat2", Seq(
  //   new WritePattern(0x100, 0x2, 0x012345678L),
  //   new WritePattern(0x500, 0x2, 0x0abcdef01L),
  //   new ReadExpectPattern(0x100, 0x2, 0x012345678L),
  //   new ReadExpectPattern(0x500, 0x2, 0x0abcdef01L)
  // )))
  // xbar.node := pusher2.node

    var seqIFU : Seq[ifu] = Seq.empty

//  if (p(ConnectIfu)) {
    val ifu    = LazyModule(new ifu("ifu"))
    xbar.node := ifu.node
    seqIFU = seqIFU :+ ifu
    //println(seqIFU.head)
    //ifu.module.io.run := true.B
  // }
//    val ifu    = LazyModule(new ifu("ifu"))
//    xbar.node := ifu.node                  //  memory.node := xbar.node

  lazy val module = new Impl
  class Impl extends LazyModuleImp(this) {
    val io = IO(new Bundle {
      val finished = Output(Bool())
    })
    pusher1.module.io.run := true.B
    // pusher2.module.io.run := true.B
    io.finished := pusher1.module.io.done
    //println(ifu)
    seqIFU.map( x => { x.module.io.run := true.B; println(x)})
    //if (p(ConnectIfu)) {
//    ifu.module.io.run := true.B
    //}
  }
}
