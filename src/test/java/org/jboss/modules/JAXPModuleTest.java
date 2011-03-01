/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.modules;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.xml.datatype.DatatypeConstants.Field;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.EventFilter;
import javax.xml.stream.Location;
import javax.xml.stream.StreamFilter;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLReporter;
import javax.xml.stream.XMLResolver;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.Comment;
import javax.xml.stream.events.DTD;
import javax.xml.stream.events.EndDocument;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.EntityDeclaration;
import javax.xml.stream.events.EntityReference;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.ProcessingInstruction;
import javax.xml.stream.events.StartDocument;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.util.XMLEventAllocator;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.URIResolver;

import org.jboss.modules.filter.PathFilter;
import org.jboss.modules.filter.PathFilters;
import org.jboss.modules.test.JAXPCaller;
import org.jboss.modules.util.TestModuleLoader;
import org.jboss.modules.util.TestResourceLoader;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Parser;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;

import __redirected.__DatatypeFactory;
import __redirected.__DocumentBuilderFactory;
import __redirected.__JAXPRedirected;
import __redirected.__SAXParserFactory;
import __redirected.__TransformerFactory;
import __redirected.__XMLEventFactory;
import __redirected.__XMLInputFactory;
import __redirected.__XMLOutputFactory;

/**
 * Tests JAXP, including all of the possible ways to trigger redirection
 *
 * @author Jason T. Greene
 */
@SuppressWarnings("deprecation")
public class JAXPModuleTest extends AbstractModuleTestCase {

    private static final ModuleIdentifier FAKE_JAXP = ModuleIdentifier.fromString("fake-jaxp");

    private TestModuleLoader moduleLoader;
    private PathFilter jdkApiFilter;


    @Before
    public void setupModuleLoader() throws Exception {
        jdkApiFilter = PathFilters.any(PathFilters.match("javax/**"),
                       PathFilters.match("org/w3c/**"));
        moduleLoader = new TestModuleLoader();

        ModuleSpec.Builder moduleWithContentBuilder = ModuleSpec.build(ModuleIdentifier.fromString("test-jaxp"));
        moduleWithContentBuilder.addResourceRoot(ResourceLoaderSpec.createResourceLoaderSpec(
                TestResourceLoader.build()
                .addClass(JAXPCaller.class)
                .create()
        ));
        moduleWithContentBuilder.addDependency(DependencySpec.createModuleDependencySpec(ModuleIdentifier.SYSTEM));
        moduleWithContentBuilder.addDependency(DependencySpec.createLocalDependencySpec());
        moduleWithContentBuilder.setMainClass(JAXPCaller.class.getName());
        moduleLoader.addModuleSpec(moduleWithContentBuilder.create());

        moduleWithContentBuilder = ModuleSpec.build(FAKE_JAXP);
        moduleWithContentBuilder.addResourceRoot(ResourceLoaderSpec.createResourceLoaderSpec(
                TestResourceLoader.build()
                .addClass(FakeSAXParserFactory.class)
                .addClass(FakeSAXParser.class)
                .addClass(FakeDocumentBuilderFactory.class)
                .addClass(FakeDocumentBuilder.class)
                .addClass(FakeTransformerFactory.class)
                .addClass(FakeTransformer.class)
                .addClass(FakeXMLEventFactory.class)
                .addClass(FakeDTD.class)
                .addClass(FakeXMLInputFactory.class)
                .addClass(FakeXMLOutputFactory.class)
                .addClass(FakeDatatypeFactory.class)
                .addClass(FakeDuration.class)
                .addResources(getResource("test/modulecontentloader/jaxp"))
                .create()
        ));
        moduleWithContentBuilder.addDependency(DependencySpec.createModuleDependencySpec(jdkApiFilter, PathFilters.rejectAll(), Module.getBootModuleLoader(),  ModuleIdentifier.SYSTEM, false));
        moduleWithContentBuilder.addDependency(DependencySpec.createLocalDependencySpec());
        moduleLoader.addModuleSpec(moduleWithContentBuilder.create());

        moduleWithContentBuilder = ModuleSpec.build(ModuleIdentifier.fromString("test-jaxp-import"));
        moduleWithContentBuilder.addResourceRoot(ResourceLoaderSpec.createResourceLoaderSpec(
                TestResourceLoader.build()
                .addClass(JAXPCaller.class)
                .create()
        ));
        moduleWithContentBuilder.addDependency(DependencySpec.createModuleDependencySpec(jdkApiFilter, PathFilters.rejectAll(), Module.getBootModuleLoader(),  ModuleIdentifier.SYSTEM, false));
        moduleWithContentBuilder.addDependency(DependencySpec.createModuleDependencySpec(PathFilters.acceptAll(), PathFilters.rejectAll(), moduleLoader, FAKE_JAXP, false));
        moduleWithContentBuilder.addDependency(DependencySpec.createLocalDependencySpec());
        moduleLoader.addModuleSpec(moduleWithContentBuilder.create());
    }

    @SuppressWarnings("unchecked")
    private static <T> T invokeMethod(Object obj, String method) {
        try {
            return (T) obj.getClass().getMethod(method).invoke(obj);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    public void testJVMDefault() throws Exception {
        ModuleClassLoader cl = moduleLoader.loadModule(ModuleIdentifier.fromString("test-jaxp")).getClassLoader();
        ClassLoader old = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(cl);
            Class<?> clazz = cl.loadClass("org.jboss.modules.test.JAXPCaller");
            checkDom(clazz, false);
            checkSax(clazz, false);
            checkTransformer(clazz, false);
            checkXmlEvent(clazz, false);
            checkXmlInput(clazz, false);
            checkXmlOutput(clazz, false);
            checkDatatype(clazz, false);
        } finally {
            Thread.currentThread().setContextClassLoader(old);
        }
    }

    @Test
    public void testReplaceDefault() throws Exception {
        __JAXPRedirected.changeAll(FAKE_JAXP, moduleLoader);

        ModuleClassLoader cl = moduleLoader.loadModule(ModuleIdentifier.fromString("test-jaxp")).getClassLoader();
        Class<?> clazz = cl.loadClass("org.jboss.modules.test.JAXPCaller");
        ClassLoader old = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(cl);
            checkDom(clazz, true);
            checkSax(clazz, true);
            checkTransformer(clazz, true);
            checkXmlEvent(clazz, true);
            checkXmlInput(clazz, true);
            checkXmlOutput(clazz, true);
            checkDatatype(clazz, true);
        } finally {
            Thread.currentThread().setContextClassLoader(old);
            __JAXPRedirected.restorePlatformFactory();
        }
    }

    @Test
    public void testImport() throws Exception {
        ModuleClassLoader cl = moduleLoader.loadModule(ModuleIdentifier.fromString("test-jaxp-import")).getClassLoader();
        Class<?> clazz = cl.loadClass("org.jboss.modules.test.JAXPCaller");
        ClassLoader old = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(cl);
            checkDom(clazz, true);
            checkSax(clazz, true);
            checkTransformer(clazz, true);
            checkXmlEvent(clazz, true);
            checkXmlInput(clazz, true);
            checkXmlOutput(clazz, true);
            checkDatatype(clazz, true);

        } finally {
            Thread.currentThread().setContextClassLoader(old);
        }
    }

    /*
     * This test is slightly dangerous. If it causes problems, just add @Ignore
     * and/or let me know.
     *   -Jason
     */
    @Test
    public void testMain() throws Throwable {
        java.lang.reflect.Field field = DefaultBootModuleLoaderHolder.class.getDeclaredField("INSTANCE");
        java.lang.reflect.Field modifiersField = java.lang.reflect.Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
        field.setAccessible(true);
        ModuleLoader oldMl = (ModuleLoader) field.get(null);
        field.set(null, moduleLoader);

        Main.main(new String[] {"-jaxpmodule", "fake-jaxp", "test-jaxp"});
        ModuleClassLoader cl = moduleLoader.loadModule(ModuleIdentifier.fromString("test-jaxp")).getClassLoader();
        Class<?> clazz = cl.loadClass("org.jboss.modules.test.JAXPCaller");
        ClassLoader old = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(cl);
            checkDom(clazz, true);
            checkSax(clazz, true);
            checkTransformer(clazz, true);
            checkXmlEvent(clazz, true);
            checkXmlInput(clazz, true);
            checkXmlOutput(clazz, true);
            checkDatatype(clazz, true);

        } finally {
            field.set(null, oldMl);
            Thread.currentThread().setContextClassLoader(old);
            __JAXPRedirected.restorePlatformFactory();
        }
    }

    public void checkDom(Class<?> clazz, boolean fake) throws Exception {
        DocumentBuilder builder = invokeMethod(clazz.newInstance(), "documentBuilder");
        DocumentBuilderFactory factory = invokeMethod(clazz.newInstance(), "documentFactory");

        Assert.assertEquals(__DocumentBuilderFactory.class.getName(), factory.getClass().getName());

        if (fake) {
            Assert.assertEquals(FakeDocumentBuilder.class.getName(), builder.getClass().getName());
        } else {
            // Double check that it works
            Document document = invokeMethod(clazz.newInstance(), "document");
            document.createElement("test");
            Assert.assertSame(DocumentBuilderFactory.newInstance().newDocumentBuilder().getClass(), builder.getClass());
        }
    }

    public void checkSax(Class<?> clazz, boolean fake) throws Exception {
        SAXParser parser = invokeMethod(clazz.newInstance(), "saxParser");
        SAXParserFactory factory = invokeMethod(clazz.newInstance(), "saxParserFactory");

        Assert.assertEquals(__SAXParserFactory.class.getName(), factory.getClass().getName());

        if (fake) {
            Assert.assertEquals(FakeSAXParser.class.getName(), parser.getClass().getName());
        } else {
            Assert.assertSame(SAXParserFactory.newInstance().newSAXParser().getClass(), parser.getClass());
        }
    }

    public void checkTransformer(Class<?> clazz, boolean fake) throws Exception {
        Transformer parser = invokeMethod(clazz.newInstance(), "transformer");
        TransformerFactory factory = invokeMethod(clazz.newInstance(), "transformerFactory");

        Assert.assertEquals(__TransformerFactory.class.getName(), factory.getClass().getName());

        if (fake) {
            Assert.assertEquals(FakeTransformer.class.getName(), parser.getClass().getName());
        } else {
            Assert.assertSame(TransformerFactory.newInstance().newTransformer().getClass(), parser.getClass());
        }
    }

    public void checkXmlEvent(Class<?> clazz, boolean fake) throws Exception {
        DTD dtd = invokeMethod(clazz.newInstance(), "eventDTD");
        XMLEventFactory factory = invokeMethod(clazz.newInstance(), "eventFactory");

        Assert.assertEquals(__XMLEventFactory.class.getName(), factory.getClass().getName());

        if (fake) {
            Assert.assertEquals(FakeDTD.class.getName(), dtd.getClass().getName());
        } else {
            Assert.assertSame(XMLEventFactory.newInstance().createDTD("blah").getClass(), dtd.getClass());
        }
    }

    public void checkXmlInput(Class<?> clazz, boolean fake) throws Exception {
        String property = invokeMethod(clazz.newInstance(), "inputProperty");
        XMLInputFactory factory = invokeMethod(clazz.newInstance(), "inputFactory");

        Assert.assertEquals(__XMLInputFactory.class.getName(), factory.getClass().getName());

        if (fake) {
            Assert.assertEquals(new FakeXMLInputFactory().getProperty("blah"), property);
        } else {
            Assert.assertFalse(new FakeXMLInputFactory().getProperty("blah").equals(property));
        }
    }

    public void checkXmlOutput(Class<?> clazz, boolean fake) throws Exception {
        String property = invokeMethod(clazz.newInstance(), "outputProperty");
        XMLOutputFactory factory = invokeMethod(clazz.newInstance(), "outputFactory");

        Assert.assertEquals(__XMLOutputFactory.class.getName(), factory.getClass().getName());

        if (fake) {
            Assert.assertEquals(new FakeXMLOutputFactory().getProperty("blah"), property);
        } else {
            Assert.assertFalse(new FakeXMLInputFactory().getProperty("blah").equals(property));
        }
    }

    public void checkDatatype(Class<?> clazz, boolean fake) throws Exception {
        Duration duration = invokeMethod(clazz.newInstance(), "duration");
        DatatypeFactory factory = invokeMethod(clazz.newInstance(), "datatypeFactory");

        Assert.assertEquals(__DatatypeFactory.class.getName(), factory.getClass().getName());

        if (fake) {
            Assert.assertEquals(new FakeDuration().getSign(), duration.getSign());
        } else {
            Assert.assertFalse(new FakeDuration().getSign() == duration.getSign());
        }
    }

    public static class FakeSAXParserFactory extends SAXParserFactory {
        public SAXParser newSAXParser() throws ParserConfigurationException, SAXException {
            return new FakeSAXParser();
        }

        public void setFeature(String name, boolean value) throws ParserConfigurationException, SAXNotRecognizedException,
                SAXNotSupportedException {
        }

        public boolean getFeature(String name) throws ParserConfigurationException, SAXNotRecognizedException,
                SAXNotSupportedException {
            return false;
        }
    }

    public static class FakeSAXParser extends SAXParser {
        @SuppressWarnings("deprecation")
        public Parser getParser() throws SAXException {
            return null;
        }

        public XMLReader getXMLReader() throws SAXException {
            return null;
        }

        public boolean isNamespaceAware() {
            return false;
        }

        public boolean isValidating() {
            return false;
        }

        public void setProperty(String name, Object value) throws SAXNotRecognizedException, SAXNotSupportedException {
        }

        public Object getProperty(String name) throws SAXNotRecognizedException, SAXNotSupportedException {
            return null;
        }
    }

    public static class FakeDocumentBuilderFactory extends DocumentBuilderFactory {
        public DocumentBuilder newDocumentBuilder() throws ParserConfigurationException {
            return new FakeDocumentBuilder();
        }

        public void setAttribute(String name, Object value) throws IllegalArgumentException {
        }

        public Object getAttribute(String name) throws IllegalArgumentException {
            return null;
        }

        public void setFeature(String name, boolean value) throws ParserConfigurationException {
        }

        public boolean getFeature(String name) throws ParserConfigurationException {
            return false;
        }
    }

    public static class FakeDocumentBuilder extends DocumentBuilder {
        public Document parse(InputSource is) throws SAXException, IOException {
            return null;
        }
        public boolean isNamespaceAware() {
            return false;
        }

        public boolean isValidating() {
            return false;
        }

        public void setEntityResolver(EntityResolver er) {
        }

        public void setErrorHandler(ErrorHandler eh) {
        }

        public Document newDocument() {
            return null;
        }

        public DOMImplementation getDOMImplementation() {
            return null;
        }
    }

    public static class FakeTransformerFactory extends TransformerFactory {
        public Transformer newTransformer(Source source) throws TransformerConfigurationException {
            return new FakeTransformer();
        }

        public Transformer newTransformer() throws TransformerConfigurationException {
            return new FakeTransformer();
        }

        public Templates newTemplates(Source source) throws TransformerConfigurationException {
            return null;
        }

        public Source getAssociatedStylesheet(Source source, String media, String title, String charset)
                throws TransformerConfigurationException {
            return null;
        }

        public void setURIResolver(URIResolver resolver) {
        }

        public URIResolver getURIResolver() {
            return null;
        }

        public void setFeature(String name, boolean value) throws TransformerConfigurationException {
        }

        public boolean getFeature(String name) {
            return false;
        }

        public void setAttribute(String name, Object value) {
        }

        public Object getAttribute(String name) {
            return null;
        }

        public void setErrorListener(ErrorListener listener) {
        }

        public ErrorListener getErrorListener() {
            return null;
        }
    }

    public static class FakeTransformer extends Transformer {

        public void transform(Source xmlSource, Result outputTarget) throws TransformerException {
        }

        public void setParameter(String name, Object value) {
        }

        public Object getParameter(String name) {
            return null;
        }

        public void clearParameters() {
        }

        public void setURIResolver(URIResolver resolver) {
        }

        public URIResolver getURIResolver() {
            return null;
        }

        public void setOutputProperties(Properties oformat) {
        }

        public Properties getOutputProperties() {
            return null;
        }

        public void setOutputProperty(String name, String value) throws IllegalArgumentException {
        }

        public String getOutputProperty(String name) throws IllegalArgumentException {
            return null;
        }

        public void setErrorListener(ErrorListener listener) throws IllegalArgumentException {
        }

        public ErrorListener getErrorListener() {
            return null;
        }

    }

    public static class FakeXMLEventFactory extends XMLEventFactory {

        public void setLocation(Location location) {
        }

        public Attribute createAttribute(String prefix, String namespaceURI, String localName, String value) {
            return null;
        }

        public Attribute createAttribute(String localName, String value) {
            return null;
        }

        public Attribute createAttribute(QName name, String value) {
            return null;
        }

        public Namespace createNamespace(String namespaceURI) {
            return null;
        }

        public Namespace createNamespace(String prefix, String namespaceUri) {
            return null;
        }

        public StartElement createStartElement(QName name, Iterator attributes, Iterator namespaces) {
            return null;
        }

        public StartElement createStartElement(String prefix, String namespaceUri, String localName) {
            return null;
        }

        public StartElement createStartElement(String prefix, String namespaceUri, String localName, Iterator attributes,
                Iterator namespaces) {
            return null;
        }

        public StartElement createStartElement(String prefix, String namespaceUri, String localName, Iterator attributes,
                Iterator namespaces, NamespaceContext context) {
            return null;
        }

        public EndElement createEndElement(QName name, Iterator namespaces) {
            return null;
        }

        public EndElement createEndElement(String prefix, String namespaceUri, String localName) {
            return null;
        }

        public EndElement createEndElement(String prefix, String namespaceUri, String localName, Iterator namespaces) {
            return null;
        }

        public Characters createCharacters(String content) {
            return null;
        }

        public Characters createCData(String content) {
            return null;
        }

        public Characters createSpace(String content) {
            return null;
        }

        public Characters createIgnorableSpace(String content) {
            return null;
        }

        public StartDocument createStartDocument() {
            return null;
        }

        public StartDocument createStartDocument(String encoding, String version, boolean standalone) {
            return null;
        }

        public StartDocument createStartDocument(String encoding, String version) {
            return null;
        }

        public StartDocument createStartDocument(String encoding) {
            return null;
        }

        public EndDocument createEndDocument() {
            return null;
        }

        public EntityReference createEntityReference(String name, EntityDeclaration declaration) {
            return null;
        }

        public Comment createComment(String text) {
            return null;
        }

        public ProcessingInstruction createProcessingInstruction(String target, String data) {
            return null;
        }

        public DTD createDTD(String dtd) {
            return new FakeDTD();
        }

    }

    public static class FakeDTD implements DTD {

        public int getEventType() {
            return 0;
        }

        public Location getLocation() {
            return null;
        }

        public boolean isStartElement() {
            return false;
        }

        public boolean isAttribute() {
            return false;
        }

        public boolean isNamespace() {
            return false;
        }

        public boolean isEndElement() {
            return false;
        }

        public boolean isEntityReference() {
            return false;
        }

        public boolean isProcessingInstruction() {
            return false;
        }

        public boolean isCharacters() {
            return false;
        }

        public boolean isStartDocument() {
            return false;
        }

        public boolean isEndDocument() {
            return false;
        }

        public StartElement asStartElement() {
            return null;
        }

        public EndElement asEndElement() {
            return null;
        }

        public Characters asCharacters() {
            return null;
        }

        public QName getSchemaType() {
            return null;
        }

        public void writeAsEncodedUnicode(Writer writer) throws XMLStreamException {
        }

        public String getDocumentTypeDeclaration() {
            return null;
        }

        public Object getProcessedDTD() {
            return null;
        }

        public List getNotations() {
            return null;
        }

        public List getEntities() {
            return null;
        }

    }

    public static class FakeXMLInputFactory extends XMLInputFactory {

        public XMLStreamReader createXMLStreamReader(Reader reader) throws XMLStreamException {
            return null;
        }

        public XMLStreamReader createXMLStreamReader(Source source) throws XMLStreamException {
            return null;
        }

        public XMLStreamReader createXMLStreamReader(InputStream stream) throws XMLStreamException {
            return null;
        }

        public XMLStreamReader createXMLStreamReader(InputStream stream, String encoding) throws XMLStreamException {
            return null;
        }

        public XMLStreamReader createXMLStreamReader(String systemId, InputStream stream) throws XMLStreamException {
            return null;
        }

        public XMLStreamReader createXMLStreamReader(String systemId, Reader reader) throws XMLStreamException {
            return null;
        }

        public XMLEventReader createXMLEventReader(Reader reader) throws XMLStreamException {
            return null;
        }

        public XMLEventReader createXMLEventReader(String systemId, Reader reader) throws XMLStreamException {
            return null;
        }

        public XMLEventReader createXMLEventReader(XMLStreamReader reader) throws XMLStreamException {
            return null;
        }

        public XMLEventReader createXMLEventReader(Source source) throws XMLStreamException {
            return null;
        }

        public XMLEventReader createXMLEventReader(InputStream stream) throws XMLStreamException {
            return null;
        }

        public XMLEventReader createXMLEventReader(InputStream stream, String encoding) throws XMLStreamException {
            return null;
        }

        public XMLEventReader createXMLEventReader(String systemId, InputStream stream) throws XMLStreamException {
            return null;
        }

        public XMLStreamReader createFilteredReader(XMLStreamReader reader, StreamFilter filter) throws XMLStreamException {
            return null;
        }

        public XMLEventReader createFilteredReader(XMLEventReader reader, EventFilter filter) throws XMLStreamException {
            return null;
        }

        public XMLResolver getXMLResolver() {
            return null;
        }

        public void setXMLResolver(XMLResolver resolver) {
        }

        public XMLReporter getXMLReporter() {
            return null;
        }

        public void setXMLReporter(XMLReporter reporter) {
        }

        public void setProperty(String name, Object value) throws IllegalArgumentException {
        }

        public Object getProperty(String name) throws IllegalArgumentException {
            return "magic-fake-thing";
        }

        public boolean isPropertySupported(String name) {
            return false;
        }

        public void setEventAllocator(XMLEventAllocator allocator) {
        }

        public XMLEventAllocator getEventAllocator() {
            return null;
        }

    }

    public static class FakeXMLOutputFactory extends XMLOutputFactory {

        public XMLStreamWriter createXMLStreamWriter(Writer stream) throws XMLStreamException {
            return null;
        }

        public XMLStreamWriter createXMLStreamWriter(OutputStream stream) throws XMLStreamException {
            return null;
        }

        public XMLStreamWriter createXMLStreamWriter(OutputStream stream, String encoding) throws XMLStreamException {
            return null;
        }

        public XMLStreamWriter createXMLStreamWriter(Result result) throws XMLStreamException {
            return null;
        }

        public XMLEventWriter createXMLEventWriter(Result result) throws XMLStreamException {
            return null;
        }

        public XMLEventWriter createXMLEventWriter(OutputStream stream) throws XMLStreamException {
            return null;
        }

        public XMLEventWriter createXMLEventWriter(OutputStream stream, String encoding) throws XMLStreamException {
            return null;
        }

        public XMLEventWriter createXMLEventWriter(Writer stream) throws XMLStreamException {
            return null;
        }

        public void setProperty(String name, Object value) throws IllegalArgumentException {
        }

        public Object getProperty(String name) throws IllegalArgumentException {
            return "magic-fake-thing";
        }

        public boolean isPropertySupported(String name) {
            return false;
        }

    }

    public static class FakeDatatypeFactory extends DatatypeFactory {
        public Duration newDuration(String lexicalRepresentation) {
            return null;
        }

        public Duration newDuration(long durationInMilliSeconds) {
            return new FakeDuration();
        }

        public Duration newDuration(boolean isPositive, BigInteger years, BigInteger months, BigInteger days, BigInteger hours,
                BigInteger minutes, BigDecimal seconds) {
            return null;
        }

        public XMLGregorianCalendar newXMLGregorianCalendar() {
            return null;
        }

        public XMLGregorianCalendar newXMLGregorianCalendar(String lexicalRepresentation) {
            return null;
        }

        public XMLGregorianCalendar newXMLGregorianCalendar(GregorianCalendar cal) {
            return null;
        }

        public XMLGregorianCalendar newXMLGregorianCalendar(BigInteger year, int month, int day, int hour, int minute,
                int second, BigDecimal fractionalSecond, int timezone) {
            return null;
        }
    }

    public static class FakeDuration extends Duration {
        public int getSign() {
            return 123456789;
        }

        public Number getField(Field field) {
            return null;
        }

        public boolean isSet(Field field) {
            return false;
        }

        public Duration add(Duration rhs) {
            return null;
        }

        public void addTo(Calendar calendar) {
        }

        public Duration multiply(BigDecimal factor) {
            return null;
        }

        public Duration negate() {
            return null;
        }

        public Duration normalizeWith(Calendar startTimeInstant) {
            return null;
        }

        public int compare(Duration duration) {
            return 0;
        }

        public int hashCode() {
            return 0;
        }
    }
}
