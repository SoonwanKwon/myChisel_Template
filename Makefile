BUILD_DIR = ./build

BASE_DIR=$(abspath ./)


PRJ = myModule
CHISEL_VERSION = chisel3

TEST ?= MyNoCTest00

PKG ?= constellation
DESIGN ?= NoC
CONFIG ?= "00"

test:
	mill -i $(PRJ)[$(CHISEL_VERSION)].test.testOnly $(PKG).$(TEST)

verilog:
	$(call git_commit, "generate verilog")
	mkdir -p $(BUILD_DIR)
	mill -i $(PRJ)[$(CHISEL_VERSION)].runMain top.TopMain --config $(CONFIG) --design $(DESIGN)

add_config:
	$(call git_commit, "generate add_config")
	mkdir -p $(BUILD_DIR)
	mill -i $(PRJ)[$(CHISEL_VERSION)].runMain top.TopMain --config $(CONFIG) --design $(DESIGN)



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
