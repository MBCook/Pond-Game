JAVAC = /System/Library/Frameworks/JavaVM.framework/Versions/1.5/Commands/javac
JAVA = /System/Library/Frameworks/JavaVM.framework/Versions/1.5/Commands/java
JAR = /System/Library/Frameworks/JavaVM.framework/Versions/1.5/Commands/jar

CLASSES = Fish.class PondGame.class PondPanel.class Water.class LillyPad.class \
			LillyPadManager.class Frog.class Fly.class Turtle.class

IMAGES = fish.gif fly.gif frog.gif lillypad-bad.gif lillypad.gif turtle.gif water.gif fly.gif

JAR_NAME = PondGame.jar

.PHONY: clean test

PondGame.jar: PondGame
	$(JAR) cmf manifest.txt $(JAR_NAME) *.class *.gif

PondGame: $(CLASSES)

all: PondGame PondGame.jar

clean:
	rm *.class PondGame.jar

test: PondGame.jar
	$(JAVA) -jar PondGame.jar

%.class: %.java
	$(JAVAC) $<
