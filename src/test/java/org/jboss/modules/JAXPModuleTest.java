/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.modules;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.xml.XMLConstants;
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
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TemplatesHandler;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import javax.xml.validation.ValidatorHandler;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathFactoryConfigurationException;
import javax.xml.xpath.XPathFunctionResolver;
import javax.xml.xpath.XPathVariableResolver;

import __redirected.__SchemaFactory;
import __redirected.__XMLReaderFactory;
import __redirected.__XPathFactory;
import __redirected.__DatatypeFactory;
import __redirected.__DocumentBuilderFactory;
import __redirected.__JAXPRedirected;
import __redirected.__SAXParserFactory;
import __redirected.__TransformerFactory;
import __redirected.__XMLEventFactory;
import __redirected.__XMLInputFactory;
import __redirected.__XMLOutputFactory;

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
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLFilter;
import org.xml.sax.XMLReader;
import sun.misc.Unsafe;

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
                       PathFilters.match("org/w3c/**"),
                       PathFilters.match("org/xml/**"));
        moduleLoader = new TestModuleLoader();

        ModuleSpec.Builder moduleWithContentBuilder = ModuleSpec.build(ModuleIdentifier.fromString("test-jaxp"));
        moduleWithContentBuilder.addResourceRoot(ResourceLoaderSpec.createResourceLoaderSpec(
                TestResourceLoader.build()
                .addClass(JAXPCaller.class)
                .create()
        ));
        moduleWithContentBuilder.addDependency(DependencySpec.createSystemDependencySpec(JDKPaths.JDK));
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
                        .addClass(FakeTransformerHandler.class)
                        .addClass(FakeXMLEventFactory.class)
                        .addClass(FakeDTD.class)
                        .addClass(FakeXMLInputFactory.class)
                        .addClass(FakeXMLOutputFactory.class)
                        .addClass(FakeDatatypeFactory.class)
                        .addClass(FakeDuration.class)
                        .addClass(FakeXPathFactory.class)
                        .addClass(FakeXPath.class)
                        .addClass(FakeSchemaFactory.class)
                        .addClass(FakeSchema.class)
                        .addClass(FakeXMLReader.class)
                        .addResources(getResource("test/modulecontentloader/jaxp"))
                        .create()
        ));
        moduleWithContentBuilder.addDependency(DependencySpec.createSystemDependencySpec(jdkApiFilter, PathFilters.rejectAll(), JDKPaths.JDK));
        moduleWithContentBuilder.addDependency(DependencySpec.createLocalDependencySpec());
        moduleLoader.addModuleSpec(moduleWithContentBuilder.create());

        moduleWithContentBuilder = ModuleSpec.build(ModuleIdentifier.fromString("test-jaxp-import"));
        moduleWithContentBuilder.addResourceRoot(ResourceLoaderSpec.createResourceLoaderSpec(
                TestResourceLoader.build()
                .addClass(JAXPCaller.class)
                .create()
        ));
        moduleWithContentBuilder.addDependency(DependencySpec.createSystemDependencySpec(jdkApiFilter, PathFilters.rejectAll(), JDKPaths.JDK));
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
    public void testReplaceDefault() throws Exception {
        __JAXPRedirected.changeAll(FAKE_JAXP, moduleLoader);

        ModuleClassLoader cl = moduleLoader.loadModule(ModuleIdentifier.fromString("test-jaxp")).getClassLoader();
        Class<?> clazz = cl.loadClass("org.jboss.modules.test.JAXPCaller");
        ClassLoader old = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(cl);
            checkDom(clazz, false, true);
            checkSax(clazz, false, true);
            checkTransformer(clazz, false, true);
            checkSAXTransformer(clazz, false, true);
            checkXPath(clazz, false, true);
            checkXmlEvent(clazz, false, true);
            checkXmlInput(clazz, false, true);
            checkXmlOutput(clazz, false, true);
            checkDatatype(clazz, false, true);
            checkSchema(clazz, false, true);
            checkXMLReader(clazz, false, true);
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
            Assert.assertTrue(JDKPaths.JDK.contains("javax/xml/parsers"));
            checkDom(clazz, true, true);
            checkSax(clazz, true, true);
            checkTransformer(clazz, true, true);
            checkSAXTransformer(clazz, true, true);
            checkXPath(clazz, true, true);
            checkXmlEvent(clazz, true, true);
            checkXmlInput(clazz, true, true);
            checkXmlOutput(clazz, true, true);
            checkDatatype(clazz, true, true);
            checkSchema(clazz, true, true);
            checkXMLReader(clazz, true, true);
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
        final java.lang.reflect.Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        Unsafe unsafe = (Unsafe) unsafeField.get(null);
        java.lang.reflect.Field field = DefaultBootModuleLoaderHolder.class.getDeclaredField("INSTANCE");
        ModuleLoader oldMl = (ModuleLoader) field.get(null);
        final Object fieldBase = unsafe.staticFieldBase(field);
        final long fieldOffset = unsafe.staticFieldOffset(field);
        unsafe.putObjectVolatile(fieldBase, fieldOffset, moduleLoader);

        Main.main(new String[] {"-jaxpmodule", "fake-jaxp", "test-jaxp"});
        ModuleClassLoader cl = moduleLoader.loadModule(ModuleIdentifier.fromString("test-jaxp")).getClassLoader();
        Class<?> clazz = cl.loadClass("org.jboss.modules.test.JAXPCaller");
        ClassLoader old = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(cl);
            checkDom(clazz, false, true);
            checkSax(clazz, false, true);
            checkTransformer(clazz, false, true);
            checkSAXTransformer(clazz, false, true);
            checkXmlEvent(clazz, false, true);
            checkXPath(clazz, false, true);
            checkXmlInput(clazz, false, true);
            checkXmlOutput(clazz, false, true);
            checkDatatype(clazz, false, true);
            checkSchema(clazz, false, true);
            checkXMLReader(clazz, false, true);
        } finally {
            unsafe.putObjectVolatile(fieldBase, fieldOffset, oldMl);
            Thread.currentThread().setContextClassLoader(old);
            __JAXPRedirected.restorePlatformFactory();
        }
    }

    public void checkDom(Class<?> clazz, boolean fakeFactory, boolean fakeImpl) throws Exception {
        DocumentBuilder builder = invokeMethod(clazz.newInstance(), "documentBuilder");
        DocumentBuilderFactory factory = invokeMethod(clazz.newInstance(), "documentFactory");

        if (fakeFactory) {
            Assert.assertEquals(FakeDocumentBuilderFactory.class.getName(), factory.getClass().getName());
        } else {
            Assert.assertEquals(__DocumentBuilderFactory.class.getName(), factory.getClass().getName());
        }
        if (fakeImpl) {
            Assert.assertEquals(FakeDocumentBuilder.class.getName(), builder.getClass().getName());
        } else {
            // Double check that it works
            Document document = invokeMethod(clazz.newInstance(), "document");
            document.createElement("test");
            Assert.assertSame(DocumentBuilderFactory.newInstance().newDocumentBuilder().getClass(), builder.getClass());
        }
    }

    public void checkSax(Class<?> clazz, boolean fakeFactory, boolean fakeImpl) throws Exception {
        SAXParser parser = invokeMethod(clazz.newInstance(), "saxParser");
        SAXParserFactory factory = invokeMethod(clazz.newInstance(), "saxParserFactory");

        if (fakeFactory) {
            Assert.assertEquals(FakeSAXParserFactory.class.getName(), factory.getClass().getName());
        } else {
            Assert.assertEquals(__SAXParserFactory.class.getName(), factory.getClass().getName());
        }
        if (fakeImpl) {
            Assert.assertEquals(FakeSAXParser.class.getName(), parser.getClass().getName());
        } else {
            Assert.assertSame(SAXParserFactory.newInstance().newSAXParser().getClass(), parser.getClass());
        }
    }

    public void checkTransformer(Class<?> clazz, boolean fakeFactory, boolean fakeImpl) throws Exception {
        Transformer parser = invokeMethod(clazz.newInstance(), "transformer");
        TransformerFactory factory = invokeMethod(clazz.newInstance(), "transformerFactory");

        if (fakeFactory) {
            Assert.assertEquals(FakeTransformerFactory.class.getName(), factory.getClass().getName());
        } else {
            Assert.assertEquals(__TransformerFactory.class.getName(), factory.getClass().getName());
        }
        if (fakeImpl) {
            Assert.assertEquals(FakeTransformer.class.getName(), parser.getClass().getName());
        } else {
            Assert.assertSame(TransformerFactory.newInstance().newTransformer().getClass(), parser.getClass());
        }
    }

    public void checkXPath(Class<?> clazz, boolean fakeFactory, boolean fakeImpl) throws Exception {
        XPath parser = invokeMethod(clazz.newInstance(), "xpath");
        XPathFactory factory = invokeMethod(clazz.newInstance(), "xpathFactory");

        if (fakeFactory) {
            Assert.assertEquals(FakeXPathFactory.class.getName(), factory.getClass().getName());
        } else {
            Assert.assertEquals(__XPathFactory.class.getName(), factory.getClass().getName());
        }
        if (fakeImpl) {
            Assert.assertEquals(FakeXPath.class.getName(), parser.getClass().getName());
        } else {
            Assert.assertSame(XPathFactory.newInstance().newXPath().getClass(), parser.getClass());
        }
    }

    public void checkSchema(Class<?> clazz, boolean fakeFactory, boolean fakeImpl) throws Exception {
        Schema parser = invokeMethod(clazz.newInstance(), "schema");
        SchemaFactory factory = invokeMethod(clazz.newInstance(), "schemaFactory");

        if (fakeFactory) {
            Assert.assertEquals(FakeSchemaFactory.class.getName(), factory.getClass().getName());
        } else {
            Assert.assertEquals(__SchemaFactory.class.getName(), factory.getClass().getName());
        }
        if (fakeImpl) {
            Assert.assertEquals(FakeSchema.class.getName(), parser.getClass().getName());
        } else {
            Assert.assertSame(SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema().getClass(), parser.getClass());
        }
    }

    public void checkXMLReader(Class<?> clazz, boolean fakeFactory, boolean fakeImpl) throws Exception {
        XMLReader parser = invokeMethod(clazz.newInstance(), "xmlReader");

        Object test = null;
        try {
            test = parser.getProperty("test");
        } catch (Exception ignore) {
        }

        if (! fakeFactory) {
            Assert.assertEquals(__XMLReaderFactory.class.getName(), parser.getClass().getName());
        }
        if (fakeImpl) {
            Assert.assertEquals("fake-fake-fake", test);
        } else {
            Assert.assertNotEquals("fake-fake-fake", test);
        }
    }

    public void checkSAXTransformer(Class<?> clazz, boolean fakeFactory, boolean fakeImpl) throws Exception {
        TransformerHandler transformerHandler = invokeMethod(clazz.newInstance(), "transformerHandler");
        TransformerFactory factory = invokeMethod(clazz.newInstance(), "transformerFactory");

        if (fakeFactory) {
            Assert.assertEquals(FakeTransformerFactory.class.getName(), factory.getClass().getName());
        } else {
            Assert.assertEquals(__TransformerFactory.class.getName(), factory.getClass().getName());
        }
        if (fakeImpl) {
            Assert.assertEquals(FakeTransformerHandler.class.getName(), transformerHandler.getClass().getName());
        } else {
            Assert.assertSame(((SAXTransformerFactory) TransformerFactory.newInstance()).newTransformerHandler().getClass(), transformerHandler.getClass());
        }
    }

    public void checkXmlEvent(Class<?> clazz, boolean fakeFactory, boolean fakeImpl) throws Exception {
        DTD dtd = invokeMethod(clazz.newInstance(), "eventDTD");
        XMLEventFactory factory = invokeMethod(clazz.newInstance(), "eventFactory");

        if (fakeFactory) {
            Assert.assertEquals(FakeXMLEventFactory.class.getName(), factory.getClass().getName());
        } else {
            Assert.assertEquals(__XMLEventFactory.class.getName(), factory.getClass().getName());
        }
        if (fakeImpl) {
            Assert.assertEquals(FakeDTD.class.getName(), dtd.getClass().getName());
        } else {
            Assert.assertSame(XMLEventFactory.newInstance().createDTD("blah").getClass(), dtd.getClass());
        }
    }

    public void checkXmlInput(Class<?> clazz, boolean fakeFactory, boolean fakeImpl) throws Exception {
        String property = invokeMethod(clazz.newInstance(), "inputProperty");
        XMLInputFactory factory = invokeMethod(clazz.newInstance(), "inputFactory");

        if (fakeFactory) {
            Assert.assertEquals(FakeXMLInputFactory.class.getName(), factory.getClass().getName());
        } else {
            Assert.assertEquals(__XMLInputFactory.class.getName(), factory.getClass().getName());
        }
        if (fakeImpl) {
            Assert.assertEquals(new FakeXMLInputFactory().getProperty("blah"), property);
        } else {
            Assert.assertNotEquals(new FakeXMLInputFactory().getProperty("blah"), property);
        }
    }

    public void checkXmlOutput(Class<?> clazz, boolean fakeFactory, boolean fakeImpl) throws Exception {
        String property = invokeMethod(clazz.newInstance(), "outputProperty");
        XMLOutputFactory factory = invokeMethod(clazz.newInstance(), "outputFactory");

        if (fakeFactory) {
            Assert.assertEquals(FakeXMLOutputFactory.class.getName(), factory.getClass().getName());
        } else {
            Assert.assertEquals(__XMLOutputFactory.class.getName(), factory.getClass().getName());
        }
        if (fakeImpl) {
            Assert.assertEquals(new FakeXMLOutputFactory().getProperty("blah"), property);
        } else {
            Assert.assertNotEquals(new FakeXMLInputFactory().getProperty("blah"), property);
        }
    }

    public void checkDatatype(Class<?> clazz, boolean fakeFactory, boolean fakeImpl) throws Exception {
        Duration duration = invokeMethod(clazz.newInstance(), "duration");
        DatatypeFactory factory = invokeMethod(clazz.newInstance(), "datatypeFactory");

        if (fakeFactory) {
            Assert.assertEquals(FakeDatatypeFactory.class.getName(), factory.getClass().getName());
        } else {
            Assert.assertEquals(__DatatypeFactory.class.getName(), factory.getClass().getName());
        }
        if (fakeImpl) {
            Assert.assertEquals(new FakeDuration().getSign(), duration.getSign());
        } else {
            Assert.assertNotEquals(new FakeDuration().getSign(), duration.getSign());
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
        public org.xml.sax.Parser getParser() throws SAXException {
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

    public static class FakeTransformerFactory extends SAXTransformerFactory {
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

        public TransformerHandler newTransformerHandler(Source src) throws TransformerConfigurationException {
            return null;
        }

        public TransformerHandler newTransformerHandler(Templates templates) throws TransformerConfigurationException {
            return null;
        }

        public TransformerHandler newTransformerHandler() throws TransformerConfigurationException {
            return new FakeTransformerHandler();
        }

        public TemplatesHandler newTemplatesHandler() throws TransformerConfigurationException {
            return null;
        }

        public XMLFilter newXMLFilter(Source src) throws TransformerConfigurationException {
            return null;
        }

        public XMLFilter newXMLFilter(Templates templates) throws TransformerConfigurationException {
            return null;
        }


    }

    private static class FakeTransformerHandler implements TransformerHandler {
        public void setResult(Result result) throws IllegalArgumentException {
        }

        public void setSystemId(String systemID) {
        }

        public String getSystemId() {
            return null;
        }

        public Transformer getTransformer() {
            return null;
        }

        public void setDocumentLocator(Locator locator) {
        }

        public void startDocument() throws SAXException {
        }

        public void endDocument() throws SAXException {
        }

        public void startPrefixMapping(String prefix, String uri) throws SAXException {
        }

        public void endPrefixMapping(String prefix) throws SAXException {
        }

        public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
        }

        public void endElement(String uri, String localName, String qName) throws SAXException {
        }

        public void characters(char[] ch, int start, int length) throws SAXException {
        }

        public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
        }

        public void processingInstruction(String target, String data) throws SAXException {
        }

        public void skippedEntity(String name) throws SAXException {
        }

        public void notationDecl(String name, String publicId, String systemId) throws SAXException {
        }

        public void unparsedEntityDecl(String name, String publicId, String systemId, String notationName) throws SAXException {
        }

        public void startDTD(String name, String publicId, String systemId) throws SAXException {
        }

        public void endDTD() throws SAXException {
        }

        public void startEntity(String name) throws SAXException {
        }

        public void endEntity(String name) throws SAXException {
        }

        public void startCDATA() throws SAXException {
        }

        public void endCDATA() throws SAXException {
        }

        public void comment(char[] ch, int start, int length) throws SAXException {
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

        public void setOutputProperties(Properties format) {
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

    public static class FakeXPathFactory extends XPathFactory {

        public boolean isObjectModelSupported(String objectModel) {
            return XPathFactory.DEFAULT_OBJECT_MODEL_URI.equals(objectModel);
        }

        public void setFeature(String name, boolean value) throws XPathFactoryConfigurationException {
        }

        public boolean getFeature(String name) throws XPathFactoryConfigurationException {
            return false;
        }

        public void setXPathVariableResolver(XPathVariableResolver resolver) {
        }

        public void setXPathFunctionResolver(XPathFunctionResolver resolver) {
        }

        public XPath newXPath() {
            return new FakeXPath();
        }
    }

    public static class FakeXPath implements XPath {

        public void reset() {
        }

        public void setXPathVariableResolver(XPathVariableResolver resolver) {
        }

        public XPathVariableResolver getXPathVariableResolver() {
            return null;
        }

        public void setXPathFunctionResolver(XPathFunctionResolver resolver) {
        }

        public XPathFunctionResolver getXPathFunctionResolver() {
            return null;
        }

        public void setNamespaceContext(NamespaceContext nsContext) {
        }

        public NamespaceContext getNamespaceContext() {
            return null;
        }

        public XPathExpression compile(String expression) throws XPathExpressionException {
            return null;
        }

        public Object evaluate(String expression, Object item, QName returnType) throws XPathExpressionException {
            return null;
        }

        public String evaluate(String expression, Object item) throws XPathExpressionException {
            return null;
        }

        public Object evaluate(String expression, InputSource source, QName returnType) throws XPathExpressionException {
            return null;
        }

        public String evaluate(String expression, InputSource source) throws XPathExpressionException {
            return null;
        }
    }

    public static class FakeSchemaFactory extends SchemaFactory {
        public boolean isSchemaLanguageSupported(String schemaLanguage) {
            return XMLConstants.W3C_XML_SCHEMA_NS_URI.equals(schemaLanguage);
        }

        public void setErrorHandler(ErrorHandler errorHandler) {
        }

        public ErrorHandler getErrorHandler() {
            return null;
        }

        public void setResourceResolver(LSResourceResolver resourceResolver) {
        }

        public LSResourceResolver getResourceResolver() {
            return null;
        }

        public Schema newSchema(Source[] schemas) throws SAXException {
            return null;
        }

        public Schema newSchema() throws SAXException {
            return new FakeSchema();
        }
    }

    public static class FakeSchema extends Schema {
        public Validator newValidator() {
            return null;
        }

        public ValidatorHandler newValidatorHandler() {
            return null;
        }
    }

    public static class FakeXMLReader implements XMLReader {
        public boolean getFeature(String name) throws SAXNotRecognizedException, SAXNotSupportedException {
            return false;
        }

        public void setFeature(String name, boolean value) throws SAXNotRecognizedException, SAXNotSupportedException {
        }

        public Object getProperty(String name) throws SAXNotRecognizedException, SAXNotSupportedException {
            return "fake-fake-fake";
        }

        public void setProperty(String name, Object value) throws SAXNotRecognizedException, SAXNotSupportedException {
        }

        public void setEntityResolver(EntityResolver resolver) {
        }

        public EntityResolver getEntityResolver() {
            return null;
        }

        public void setDTDHandler(DTDHandler handler) {
        }

        public DTDHandler getDTDHandler() {
            return null;
        }

        public void setContentHandler(ContentHandler handler) {
        }

        public ContentHandler getContentHandler() {
            return null;
        }

        public void setErrorHandler(ErrorHandler handler) {
        }

        public ErrorHandler getErrorHandler() {
            return null;
        }

        public void parse(InputSource input) throws IOException, SAXException {
        }

        public void parse(String systemId) throws IOException, SAXException {
        }
    }
}
