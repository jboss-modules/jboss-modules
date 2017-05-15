

all: target/Manual.html

target:
	mkdir -p target

target/Manual.html: src/Manual.adoc Makefile target
	asciidoctor -a toc -b html5 src/Manual.adoc -o target/Manual.html
