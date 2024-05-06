package freenmp

import chiseltest.{ChiselScalatestTester, VerilatorBackendAnnotation, WriteVcdAnnotation}
import firrtl.AnnotationSeq
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

abstract class NMPBaseTester extends AnyFlatSpec with ChiselScalatestTester with Matchers with HasTestAnnos {
  behavior of "Free NMP"
  val defaultConfig = new MyBaseConfig
  implicit val config = defaultConfig.alterPartial({
    case CoreParamsKey => CoreParameters().copy (
      	nRows = 64,
      	nCols = 64
    )
  }
  )
}

trait HasTestAnnos {
  var testAnnos : AnnotationSeq = Seq()
}

trait DumpVCD { this: HasTestAnnos =>
  testAnnos = testAnnos :+ WriteVcdAnnotation
}

trait UseVerilatorBackend { this: HasTestAnnos =>
  testAnnos = testAnnos :+ VerilatorBackendAnnotation
}
