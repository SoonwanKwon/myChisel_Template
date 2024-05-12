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

class IbufIo (implicit p: Parameters) extends nmpBundle {
  val Data = Vec(nVT, UInt((rows*ifmBits).W))
  val Valid = Vec(nVT, Bool())
  val isZero  = Vec(nVT,Bool())
}

class Ibuf2PGIo (implicit p: Parameters) extends nmpBundle {
  val ifm = Vec(nXbarInPE, Vec(rows, UInt(iBitsPerCycle.W)))
  val valid = Vec(nXbarInPE, Bool())
}


class ProcessingElement (implicit val p: Parameters) extends Module with HasNMPParameter {

 // lazy val module = new Impl
  //class Impl  extends LazyModuleImp(this) {

    val xbars  = for (nn <-0 until nXbarInPE) yield {
      Module(new XBAR(rows, cols, weightBits, xbarOutBits, iBitsPerCycle))
    }
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
      m.ioIn <> ioIn
      m.ioIn.wen := io.iXbarEn(idx) & ioIn.wen
      m.ioIn.ren := io.iXbarEn(idx)  & ioIn.ren
      m.io.idata := io.idata(idx)
      m.io.cen := io.iXbarEn(idx) & io.cen
    }}
    for (cc <- 0 until rows)
      outBuf(cc) := xbars.map(_.ioOut.outData(cc)).reduce(_ +& _)
    ioOut.outData := outBuf
    outValid := xbars.map(_.ioOut.outValid).reduce(_ ||_ )
    ioOut.outValid := outValid
//  } // end of Impl

}

class BufferPG (implicit val p: Parameters) extends Module with HasNMPParameter {

  // no node yet

//  lazy val module = new Impl

//  class Impl  extends LazyModuleImp(this) {

    val io = IO(new Bundle {
      val in = Input(new IbufIo)
      val out = Output(new IbufIo)
      val toPG = Output(new Ibuf2PGIo)
      val sel = Input(UInt(2.W)) // FixMe
      val push = Input(Bool())
    })

    val nReg = Reg(Vec(nVT, UInt((rows * ifmBits).W)))
    val validReg = RegInit(VecInit(Seq.fill(nVT)(false.B)))

    (0 until nVT).foreach { idx =>
      when(io.push) {
        nReg(idx) := io.in.Data(idx)
        validReg(idx) := io.in.Valid(idx)
      }
    }

    io.out <> io.in

    // Selection
    val idxMax = nVT - nXbarInPE
    (0 until idxMax).foreach(idx =>
      when(io.sel === idx.U) {
        for (jj <- 0 until nXbarInPE) {
          io.toPG.ifm(jj) := nReg(idx + jj)
          io.toPG.valid(jj) := validReg(idx + jj)
        }
      }
    )
//  }
}

class ProcessingGroup (implicit p: Parameters) extends LazyModule with HasNMPParameter {

  lazy val module = new Impl

  class Impl  extends LazyModuleImp(this) {

    val io = IO(new Bundle {

    })

    //
    val pes = for (i <- 0 until nPEInPG) yield {
      Module (new ProcessingElement())
    }

    val ibuf = Module (new BufferPG())

    // All PE share same ifm
    for (idx <- 0 until nXbarInPE) {
      pes(idx).io.idata := ibuf.io.toPG.ifm
    }

  }
}
