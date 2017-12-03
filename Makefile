IMGFILES=$(patsubst %,target/%,$(wildcard img/*.png))

all: target/Manual.html target/highlight $(IMGFILES)

target:
	mkdir target

target/highlight: target highlight
	rm -rf target/highlight
	cp -r highlight target/

target/img: target
	mkdir -p target/img

target/img/%.png: img/%.png target/img target/highlight
	cp $< $@

target/Manual.html: src/Manual.adoc Makefile target
	asciidoctor -a toc -b html5 src/Manual.adoc -o target/Manual.html
