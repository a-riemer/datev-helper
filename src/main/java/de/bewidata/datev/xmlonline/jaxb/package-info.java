/**
 * JAXB-Modellklassen für die DATEV XML-Schnittstelle online (document.xml, Schema v6.0).
 * Namespace: http://xml.datev.de/bedi/tps/document/v06.0
 */
@XmlSchema(
        namespace = "http://xml.datev.de/bedi/tps/document/v06.0",
        elementFormDefault = XmlNsForm.QUALIFIED,
        xmlns = {
                @XmlNs(prefix = "",    namespaceURI = "http://xml.datev.de/bedi/tps/document/v06.0"),
                @XmlNs(prefix = "xsi", namespaceURI = "http://www.w3.org/2001/XMLSchema-instance")
        }
)
package de.bewidata.datev.xmlonline.jaxb;

import jakarta.xml.bind.annotation.XmlNs;
import jakarta.xml.bind.annotation.XmlNsForm;
import jakarta.xml.bind.annotation.XmlSchema;
