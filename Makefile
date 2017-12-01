IMGFILES=$(patsubst %,target/%,$(wildcard img/*.png))

all: target/Manual.html $(IMGFILES)

target:
	mkdir target

target/img: target
	mkdir target/img

target/img/%.png: img/%.png target/img
	cp $< $@

target/Manual.html: src/Manual.adoc Makefile target
	asciidoctor -a toc -b html5 src/Manual.adoc -o target/Manual.html
