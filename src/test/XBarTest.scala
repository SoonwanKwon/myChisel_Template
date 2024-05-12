package freenmp.test

import chisel3._
import chisel3.util.{Cat, Decoupled, Queue, log2Ceil}
import chiseltest.WriteVcdAnnotation
import chiseltest.simulator.{VerilatorBackendAnnotation, VerilatorCFlags, VerilatorFlags}
import firrtl.options.TargetDirAnnotation
import org.chipsalliance.cde.config.Parameters
import freechips.rocketchip.diplomacy.{LazyModule, LazyModuleImp, NexusNode, RenderedEdge, SimpleNodeImp, SinkNode, SourceNode, ValName}
import freechips.rocketchip.util._
import freenmp._
import chiseltest._

class XbarTest extends NMPBaseTester {
  behavior of "XBAR"
  it should "pass" in {
    //val ldut = Module(new ProcessingElement()(config))
    //val ldut = LazyModule(new ProcessingElement()(config))
    test(new ProcessingElement()(config)).withAnnotations(Seq(
      VerilatorBackendAnnotation,
      VerilatorFlags(Seq("--trace")),
      VerilatorCFlags(Seq("")),
      WriteVcdAnnotation,
      TargetDirAnnotation("./build")
    )) { dut =>
      val cfg = config(CoreParamsKey)

      def setIdle() = {
        for(ii <-0 until 3) // FixMe
          dut.io.iXbarEn(ii).poke(true.B)
        dut.io.cen.poke(false.B)
        dut.ioIn.wen.poke(false.B)
       dut.ioIn.ren.poke(false.B)
        dut.clock.step(1)
      }

      def writeWeightRow(xbarEn:Seq[Boolean], row: Int, wval: Int) = {
        xbarEn.zipWithIndex.foreach { case (en, ii) =>
          dut.io.iXbarEn(ii).poke(en.B)
        }
        dut.ioIn.wen.poke(true.B)
        dut.ioIn.ren.poke(false.B)
        dut.io.cen.poke(false.B)
        dut.ioIn.waddr.poke(row)
        for (rr <- 0 until cfg.nRows)
          dut.ioIn.wdata(rr).poke( (wval).S)
        dut.clock.step(1)
      }


      def extractBits(value: Int, bits: Int): Seq[Int] = {
        (0 until bits).map { bitPosition =>
          (value >> bitPosition) & 1
        }
      }
      def compute(ctrl: Seq[Boolean], in: Seq[Seq[Int]]) = {
        dut.ioIn.wen.poke(false.B)
        dut.ioIn.ren.poke(false.B)
        dut.io.cen.poke(true.B)
        ctrl.zipWithIndex.foreach {
          case (dd, ii) =>
            dut.io.iXbarEn(ii).poke(dd.B)
        }

          for (op_cycle <- 0 until 4) {
            in.zipWithIndex.foreach { case (ind, xbarIdx) =>
              val DecomposedMAP = ind.map(s => extractBits(s, 4))
              DecomposedMAP.zipWithIndex.foreach { case (m, idx) =>
                dut.io.idata(xbarIdx)(idx).poke(m(3 - op_cycle).U)
              }
              dut.ioIn.first.poke((op_cycle == 0).B)
              dut.ioIn.shft.poke(true.B)
              dut.ioIn.neg.poke((op_cycle == 0).B)
              dut.ioIn.last.poke((op_cycle == 3).B)
            }
            dut.clock.step(1)
          }
      }

      setIdle()
      dut.clock.step(10)
      for (i <- 0 until cfg.nRows) {
        writeWeightRow(Seq(true, false, false), i, 3) // write to xbar 0
      }
      for (i <- 0 until cfg.nRows) {
        writeWeightRow(Seq(false, true, false), i, -3) // write to xbar 1
      }
      for (i <- 0 until cfg.nRows) {
        writeWeightRow(Seq(false, false, true), i, 7) // write to xbar 2
      }
      setIdle()

      val in0 = Seq.fill(cfg.nRows)(4)
      val in1 = Seq.fill(cfg.nRows)(-3)
      val in2 = Seq.fill(cfg.nRows)(7)
      val in3 = Seq.fill(cfg.nRows)(-7)
      println("Comp Test")
      println(extractBits(4,4))

      compute(Seq(true, false, false), Seq(in0,in1, in2))
      compute(Seq(true, false, true), Seq(in0,in1, in2))
      compute(Seq(true, true, true), Seq(in0,in1, in2))
      compute(Seq(true, true, true), Seq(in2,in0, in1))
      setIdle()
      dut.clock.step(10)
    }
  }
}


