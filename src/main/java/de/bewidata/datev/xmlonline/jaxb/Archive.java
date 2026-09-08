package de.bewidata.datev.xmlonline.jaxb;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Root-Element der DATEV document.xml (XML-Schnittstelle online, Schema v6.0).
 * Namespace: http://xml.datev.de/bedi/tps/document/v06.0
 */
@XmlRootElement(name = "archive", namespace = "http://xml.datev.de/bedi/tps/document/v06.0")
@XmlAccessorType(XmlAccessType.FIELD)
public class Archive {

    @XmlAttribute(name = "version", required = true)
    private String version = "6.0";

    @XmlAttribute(name = "guid")
    private String guid;

    @XmlAttribute(name = "generatingSystem")
    private String generatingSystem;

    @XmlElement(name = "header", required = true)
    private Header header;

    @XmlElement(name = "content", required = true)
    private Content content;

    public String getVersion() { return version; }
    public String getGuid() { return guid; }
    public void setGuid(String guid) { this.guid = guid; }
    public String getGeneratingSystem() { return generatingSystem; }
    public void setGeneratingSystem(String generatingSystem) { this.generatingSystem = generatingSystem; }
    public Header getHeader() { return header; }
    public void setHeader(Header header) { this.header = header; }
    public Content getContent() { return content; }
    public void setContent(Content content) { this.content = content; }
}
