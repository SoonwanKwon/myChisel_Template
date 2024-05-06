package freenmp
import chisel3._
import chisel3.util._
import org.chipsalliance.cde.config._


class	MyBaseConfig extends Config((site, here, up) => {
	case CoreParamsKey => CoreParameters()
})
