/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017 Red Hat, Inc., and individual contributors
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

package __redirected;

import java.util.function.Supplier;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.validation.SchemaFactory;
import javax.xml.xpath.XPathFactory;

import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

final class JDKSpecific {

    static Supplier<DatatypeFactory> getPlatformDatatypeFactorySupplier() {
        return new Supplier<DatatypeFactory>() {
            public DatatypeFactory get() {
                return DatatypeFactory.newDefaultInstance();
            }
        };
    }

    static Supplier<DocumentBuilderFactory> getPlatformDocumentBuilderFactorySupplier() {
        return new Supplier<DocumentBuilderFactory>() {
            public DocumentBuilderFactory get() {
                return DocumentBuilderFactory.newDefaultInstance();
            }
        };
    }

    static Supplier<SAXParserFactory> getPlatformSaxParserFactorySupplier() {
        return new Supplier<SAXParserFactory>() {
            public SAXParserFactory get() {
                return SAXParserFactory.newDefaultInstance();
            }
        };
    }

    static Supplier<SchemaFactory> getPlatformSchemaFactorySupplier() {
        return new Supplier<SchemaFactory>() {
            public SchemaFactory get() {
                return SchemaFactory.newDefaultInstance();
            }
        };
    }

    static Supplier<TransformerFactory> getPlatformSaxTransformerFactorySupplier() {
        return new Supplier<TransformerFactory>() {
            public TransformerFactory get() {
                return TransformerFactory.newDefaultInstance();
            }
        };
    }

    static Supplier<XMLEventFactory> getPlatformXmlEventFactorySupplier() {
        return new Supplier<XMLEventFactory>() {
            public XMLEventFactory get() {
                return XMLEventFactory.newDefaultFactory();
            }
        };
    }

    static Supplier<XMLInputFactory> getPlatformXmlInputFactorySupplier() {
        return new Supplier<XMLInputFactory>() {
            public XMLInputFactory get() {
                return XMLInputFactory.newDefaultFactory();
            }
        };
    }

    static Supplier<XMLOutputFactory> getPlatformXmlOutputFactorySupplier() {
        return new Supplier<XMLOutputFactory>() {
            public XMLOutputFactory get() {
                return XMLOutputFactory.newDefaultFactory();
            }
        };
    }

    static Supplier<XMLReader> getPlatformXmlReaderSupplier() {
        return new Supplier<XMLReader>() {
            public XMLReader get() {
                try {
                    return SAXParserFactory.newDefaultInstance().newSAXParser().getXMLReader();
                } catch (SAXException | ParserConfigurationException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    static Supplier<XPathFactory> getPlatformXPathFactorySupplier() {
        return new Supplier<XPathFactory>() {
            public XPathFactory get() {
                return XPathFactory.newDefaultInstance();
            }
        };
    }
}
