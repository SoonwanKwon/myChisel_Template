package top

import chisel3.stage.{
  ChiselGeneratorAnnotation,
  ChiselStage
}

import firrtl.{
  AttributeAnnotation,
  DescriptionAnnotation,
  DocStringAnnotation,
  EmittedCircuitAnnotation
}

object Generator {
  def execute(args: Array[String], mod: => chisel3.RawModule, firtoolOps: Array[String], design: String, targetDir: String)= {
    // 1st Chisel, but not FIRRTL
    val annotations = (new ChiselStage).execute(args, Seq(ChiselGeneratorAnnotation( () => mod)))
    println("Chisel produces the following'DescriptionAnnotatoin':")
    annotations
      .foreach {
        case a: DescriptionAnnotation => println(s"   -${a.serialize}")
        case a =>
      }

    // 2nd FIRRTL
    import sys.process._
    val genSV = Seq("/home/kswan7004/tools/firtool-1.74.0/bin/firtool",
      "--O=debug",
      "--format=fir",
      "--split-verilog",
      s"--blackbox-path=src/main/resources",
      "--disable-all-randomization",
//      "--emit-chisel-asserts-as-sva",
      "--add-mux-pragmas",
      "--fixup-eicg-wrapper",
      "--emit-separate-always-blocks",
      "--export-module-hierarchy",
      "--verify-each=true",
      "--preserve-values=all",
      "--lowering-options=explicitBitcast",
      "--lowering-options=disallowLocalVariables",
      "--lowering-options=disallowExpressionInliningInPorts",
      "--lowering-options=caseInsensitiveKeywords",
      "--repl-seq-mem", s"--repl-seq-mem-file=sram.conf",
      s"--export-chisel-interface --chisel-interface-out-dir=${targetDir}",
      s"-o ${targetDir}/rtl_codes ${targetDir}/${design}.fir",
      ).reduce(_ + " "+ _)
    println(genSV)
    genSV.!
  }
}
