package de.bewidata.datev.xmlonline.jaxb;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;

import java.util.ArrayList;
import java.util.List;

/**
 * Ein Dokument (Beleggruppe) in der document.xml.
 * Das guid-Attribut verknüpft das Dokument mit dem Buchungssatz über
 * das CSV-Feld "Beleglink" (Wert: "BEDI \"" + guid + "\"").
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class Document {

    /** UUID des Belegs; erscheint im CSV als "BEDI \"{guid}\"" */
    @XmlAttribute(name = "guid")
    private String guid;

    /** 1 = Rechnungseingang, 2 = Rechnungsausgang */
    @XmlAttribute(name = "type")
    private Byte type;

    /** 1 = Buchungsrelevant (Posteingang), 2 = Archivierungsrelevant */
    @XmlAttribute(name = "processID")
    private Byte processID;

    @XmlElement(name = "description")
    private String description;

    @XmlElement(name = "keywords")
    private String keywords;

    @XmlElement(name = "extension", required = true)
    private List<Extension> extensions = new ArrayList<>();

    @XmlElement(name = "repository")
    private Repository repository;

    public String getGuid() { return guid; }
    public void setGuid(String guid) { this.guid = guid; }
    public Byte getType() { return type; }
    public void setType(Byte type) { this.type = type; }
    public Byte getProcessID() { return processID; }
    public void setProcessID(Byte processID) { this.processID = processID; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getKeywords() { return keywords; }
    public void setKeywords(String keywords) { this.keywords = keywords; }
    public List<Extension> getExtensions() { return extensions; }
    public Repository getRepository() { return repository; }
    public void setRepository(Repository repository) { this.repository = repository; }
}
