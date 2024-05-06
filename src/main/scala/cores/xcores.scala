package freenmp

import chisel3._
import chisel3.util.BitPat.fromUIntToBitPatComparable
import chisel3.util.log2Ceil
import freechips.rocketchip.diplomacy.{LazyModule, LazyModuleImp}
import org.chipsalliance.cde.config.Parameters

// Bundle for Buffer IO
abstract class nmpBundle(implicit val p: Parameters) extends Bundle
with HasNMPParameter
//

class IbufIo extends nmpBundle {
  val Data = Vec(nVT, UInt((rows*ifmBits).W))
  val Valid = Vec(nVT, Bool)
  val isZero  = Vec(nVT,Bool)
}

class Ibuf2PGIo extends nmpBundle {
  val ifm = Vec(nXbarInPE, Vec(rows, UInt(iBitsPerCycle).W))
}


class ProcessingElement (implicit p: Parameters) extends LazyModule with HasNMPParameter {

  val xbars  = for (nn <-0 until nXbarInPE) yield {
    LazyModule(new XBAR())
  }

  lazy val module = new Impl

  class Impl  extends LazyModuleImp(this) {
    val io = IO(new Bundle {
      val idata = Input(Vec(nXbarInPE,Vec(rows, UInt(iBitsPerCycle.W))))
      val iXbarEn = Input(Vec(nXbarInPE, Bool()))
      val cen = Input(Bool()) // Compute Enable
    })
    val ioIn = IO(new Bundle {
      val wdata = Input(Vec(rows, SInt(weightBits.W)))
      val wen = Input(Bool()) // Write weight
      val ren = Input(Bool()) // read weight
      val neg = Input(Bool()) // Negate (for Sign)
      val shft = Input(Bool())
      val first = Input(Bool())
      val last = Input(Bool())
      val waddr = Input(UInt(log2Ceil(rows).W)) // row address for write weight
    })
    val ioOut = IO(new Bundle {
      val	outData = Output(Vec(cols, SInt((xbarOutBits+log2Ceil(nXbarInPE)).W )))
      val outValid = Output(Bool())
    })

    val outBuf = Reg(Vec(cols, SInt((xbarOutBits + log2Ceil(nXbarInPE)).W)))
    val outValid = Reg(Bool())

    xbars.zipWithIndex.foreach{ case (m, idx) => {
      m.module.ioIn <> ioIn
      m.module.ioIn.wen := io.iXbarEn(idx) & ioIn.wen
      m.module.ioIn.ren := io.iXbarEn(idx)  & ioIn.ren
      m.module.io.idata := io.idata(idx)
      m.module.io.cen := io.iXbarEn(idx) & io.cen
    }}
    for (cc <- 0 until rows)
      outBuf(cc) := xbars.map(_.module.ioOut.outData(cc)).reduce(_ +& _)
    ioOut.outData := outBuf
    outValid := xbars.map(_.module.ioOut.outValid).reduce(_ ||_ )
    ioOut.outValid := outValid
  } // end of Impl

}

class BufferPG (implicit p: Parameters) extends LazyModule with HasNMPParameter {
  val io = IO(new Bundle{
    val in = Input(new IbufIo)
    val out = Output(new IbufIo)
    val toPG = Output(new Ibuf2PGIo)
    val sel = Input(UInt(2.W)) // FixMe
    val push = Input(Bool)
  })

  val nReg = Reg(Vec(nVT, UInt((rows*ifmBits).W)))

  (0until nVT).foreach {
    case (idx) => {
      when(io.push & io.in.Valid(idx)) {
        nReg(idx) := io.in.Data(idx)
      }
    }
  }

  }
}