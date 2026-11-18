INPUT_FILE := ../../inputs/sample-input.txt
# RUN_CLASS = utd.aos.p2.Test
RUN_CLASS = utd.aos.p2.Main

SRC_DIR := src/main/java
BUILD_DIR := target

SOURCES := $(shell find $(SRC_DIR) -name '*.java')
CLASSES := $(patsubst $(SRC_DIR)/%.java,$(BUILD_DIR)/%.class,$(SOURCES))

# JDK22_BIN_DIR := /usr/local/jdk-22.0.1/bin
ifneq ($(findstring utdallas.edu,$(shell hostname -f)),)
    JDK22_BIN_DIR := /usr/local/jdk-22.0.1/bin/
else
    JDK22_BIN_DIR :=
endif

JAVA := $(JDK22_BIN_DIR)java
JAVAC := $(JDK22_BIN_DIR)javac

# parallel build using all available CPU cores!
MAKEFLAGS += -j$(shell nproc)

.PHONY: build run clean

build: $(CLASSES)

$(BUILD_DIR)/%.class: $(SRC_DIR)/%.java
	@mkdir -p $(dir $@)
	$(JAVAC) -cp $(BUILD_DIR) -sourcepath $(SRC_DIR) -d $(BUILD_DIR) $<

run: build
	$(JAVA) -cp $(BUILD_DIR) $(RUN_CLASS) $(INPUT_FILE)

clean:
	rm -rf $(BUILD_DIR)

zip:
	@echo "uhh TODO: write the correct zip args"
	# zip -r ../$(BINARY_NAME)-$(NETID).zip . -x ".gitignore" -x ".git/*" -x TODO.md -x "$(BUILD_DIR)/*" -x ".cache/*" -x "bench-results/*"

