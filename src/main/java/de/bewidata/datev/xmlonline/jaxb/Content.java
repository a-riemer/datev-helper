package de.bewidata.datev.xmlonline.jaxb;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

import java.util.ArrayList;
import java.util.List;

/** Inhaltsbereich der document.xml – enthält alle Dokument-Einträge. */
@XmlAccessorType(XmlAccessType.FIELD)
public class Content {

    @XmlElement(name = "document", required = true)
    private List<Document> documents = new ArrayList<>();

    public List<Document> getDocuments() { return documents; }
}
