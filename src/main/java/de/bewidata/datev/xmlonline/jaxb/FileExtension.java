package de.bewidata.datev.xmlonline.jaxb;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

/** Belegbild-Datei im ZIP-Archiv (extension type="File"). */
@XmlType(name = "File", namespace = "http://xml.datev.de/bedi/tps/document/v06.0")
@XmlAccessorType(XmlAccessType.FIELD)
public class FileExtension extends Extension {

    @XmlAttribute(name = "name", required = true)
    private String name;

    public FileExtension() {}

    public FileExtension(String name) {
        this.name = name;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
