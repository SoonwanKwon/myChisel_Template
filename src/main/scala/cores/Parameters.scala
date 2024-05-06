package freenmp

import org.chipsalliance.cde.config.{Field, Parameters}
import chisel3._
import chisel3.util._

case object CoreParamsKey extends Field[CoreParameters]

case class CoreParameters 
(
	nRows: Int = 64,
	nCols: Int = 64,
	nFeatureBits: Int = 4,
	nWeightBits: Int = 4,
	nOutBits: Int = 7,
	nInParallel: Int = 1
)


trait	HasNMPParameter {
	implicit val p: Parameters

	val	xbarParams = p(CoreParamsKey)

	val	rows = xbarParams.nRows
	val cols = xbarParams.nCols
	val ifmBits = xbarParams.nFeatureBits
	val weightBits = xbarParams.nWeightBits
	val xbarOutBits = xbarParams.nOutBits
	val iBitsPerCycle = xbarParams.nInParallel

	val nXbarInPE = 3
	val nVT = 4 // Vertical Traverse
}
