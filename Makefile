addLevel = src $(foreach dir,$(1),$(wildcard $(dir)/*))
addLevelSq = $(call addLevel,$(call addLevel,$(1)))
dirs = $(call addLevelSq,$(call addLevelSq,$(call addLevelSq,src)))
sources = $(filter %.java,$(dirs))
objects := $(patsubst src/%.java,bin/%.class,$(sources))

LandscapeGenerator.jar : $(objects) Manifest.txt
	jar -cfme LandscapeGenerator.jar Manifest.txt _4denthusiast.landscapegenerator.Main -C bin .

bin/%.class : src/%.java
	javac -cp bin:lwjgl/jar/lwjgl.jar:lwjgl/jar/lwjgl_util.jar -sourcepath src -d bin -Xlint:unchecked -Xlint:deprecation $<
