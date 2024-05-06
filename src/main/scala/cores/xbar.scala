package freenmp

import chisel3._
import chisel3.util.{Cat, Decoupled, Fill, Queue, log2Ceil}
import org.chipsalliance.cde.config.Parameters
import freechips.rocketchip.diplomacy.{LazyModule, LazyModuleImp, NexusNode, RenderedEdge, SimpleNodeImp, SinkNode, SourceNode, ValName}
import freechips.rocketchip.util._

class XBAR (implicit p: Parameters) extends LazyModule with HasNMPParameter {

	lazy val module = new Impl
	class Impl extends LazyModuleImp(this) {
		val io = IO(new Bundle {
			val idata = Input(Vec(rows, UInt(iBitsPerCycle.W)))
			val cen = Input(Bool()) // Compute Enable
		})
		val	ioIn = IO(new Bundle {
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
			val	outData = Output(Vec(cols, SInt(xbarOutBits.W)))
			val outValid = Output(Bool())
		})

		val BeatBits = iBitsPerCycle * weightBits + log2Ceil(rows)
		val fullBits = iBitsPerCycle * weightBits + log2Ceil(rows) + 4 // consider accum
		val outMSB = BeatBits-1
		val outLSB = outMSB - (xbarOutBits - 1)
	
		val	wReg = Reg(Vec(rows, Vec(cols, SInt(weightBits.W))))
		val	oReg = Reg(Vec(cols, SInt((fullBits).W)))
	
		when(ioIn.wen & !io.cen & !ioIn.ren) {	// write to weight register
			wReg(ioIn.waddr) := ioIn.wdata
		}
	
		// read
		for (cc <- 0 until cols) {
			when(!ioIn.wen & !io.cen & ioIn.ren) {
				oReg(cc) := wReg(ioIn.waddr)(cc)
			}
		}
		// Function to recursively build an adder tree
		def adderTree(values: Seq[SInt]): SInt = {
			if (values.length == 1) {
				values.head
			} else {
				val pairedSums = values.grouped(2).map {
					case Seq(a, b) => a +& b
						case Seq(a) => a
				}.toSeq
				adderTree(pairedSums) // Recurse with the results
			}
		}
		// Compute
		val computedOut: Seq[SInt] = {
			for (cc <- 0 until cols) yield {
				val cRes = for(rr <-0 until rows) yield {
					(io.idata(rr) * wReg(rr)(cc))
				}
				adderTree(cRes) //.reduce( _ +& _)
			}
		}

		// Output
		for (cc <-0 until cols) {
			when(!ioIn.wen & io.cen & !ioIn.ren) {
				val result_t = computedOut(cc)
				val result = Mux(ioIn.neg, ~result_t+1.S,
					result_t)     // Mux(valid, result, UIntToOH1(result,xbarOutBits))
				//val valid = (result < UIntToOH1(result, xbarOutBits))
				when(ioIn.first) {
					oReg(cc) := result //set
				}
				.otherwise {
					oReg(cc) := (oReg(cc) << ioIn.shft )+ result // accumulated
				}
			}
		}

		when(io.cen === false.B) {
			oReg := 0.U.asTypeOf(oReg)
		}

		for(cc <- 0 until rows)
			ioOut.outData(cc) := oReg(cc)(fullBits-1, fullBits-xbarOutBits).asSInt
		ioOut.outValid := RegNext(io.cen & ioIn.last)
	}
}


class	NMPTestHarness()(implicit p: Parameters) extends LazyModule {
	lazy val module = new Impl
	class Impl extends LazyModuleImp(this) {
		val io = IO(new Bundle {
			val success = Output(Bool())

		})
		val ldut = LazyModule(new XBAR()(p))

		val rCnt = RegInit(0.U(8.W))
		
		io.success :=rCnt(7)
	}
}
