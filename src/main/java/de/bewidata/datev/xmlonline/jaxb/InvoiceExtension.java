package de.bewidata.datev.xmlonline.jaxb;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

/** Strukturierte Rechnungsdaten (extension type="Invoice"). */
@XmlType(name = "Invoice", namespace = "http://xml.datev.de/bedi/tps/document/v06.0")
@XmlAccessorType(XmlAccessType.FIELD)
public class InvoiceExtension extends Extension {

    @XmlAttribute(name = "datafile", required = true)
    private String datafile;

    @XmlElement(name = "property", required = true)
    private InvoiceProperty property;

    public InvoiceExtension() {}

    public InvoiceExtension(String datafile, String invoiceType) {
        this.datafile = datafile;
        this.property = new InvoiceProperty(invoiceType);
    }

    public String getDatafile() { return datafile; }
    public void setDatafile(String datafile) { this.datafile = datafile; }
    public InvoiceProperty getProperty() { return property; }
    public void setProperty(InvoiceProperty property) { this.property = property; }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class InvoiceProperty {
        @XmlAttribute(name = "key", required = true)
        private final String key = "InvoiceType";
        /** "Incoming" = Rechnungseingang, "Outgoing" = Rechnungsausgang */
        @XmlAttribute(name = "value", required = true)
        private String value;

        public InvoiceProperty() {}
        public InvoiceProperty(String value) { this.value = value; }
        public String getKey() { return key; }
        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
    }
}
