BUILD_DIR = ./build

PRJ = myModule
CHISEL_VERSION = chisel3

test:
	mill -i $(PRJ)[$(CHISEL_VERSION)].test

verilog:
	$(call git_commit, "generate verilog")
	mkdir -p $(BUILD_DIR)
	mill -i $(PRJ)[$(CHISEL_VERSION)].runMain core_complex.Generator --target-dir $(BUILD_DIR)

core_complex:
	$(call git_commit, "generate verilog")
	mkdir -p $(BUILD_DIR)
	mill -i $(PRJ)[$(CHISEL_VERSION)].runMain core_complex.Generator --target-dir $(BUILD_DIR)

help:
	mill -i $(PRJ)[$(CHISEL_VERSION)].runMain adder_config.Generator --help

reformat:
	mill -i __.reformat

checkformat:
	mill -i __.checkFormat

clean:
	-rm -rf $(BUILD_DIR)

.PHONY: test verilog help reformat checkformat clean

sim:
	$(call git_commit, "sim RTL") # DO NOT REMOVE THIS LINE!!!
	@echo "Write this Makefile by yourself."

-include ../Makefile
