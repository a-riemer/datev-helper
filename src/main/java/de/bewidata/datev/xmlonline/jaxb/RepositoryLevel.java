package de.bewidata.datev.xmlonline.jaxb;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;

/** Eine Hierarchieebene der Ablagestruktur in Belege online. */
@XmlAccessorType(XmlAccessType.FIELD)
public class RepositoryLevel {

    /** 1 = Kategorie, 2 = Ordner, 3 = Register */
    @XmlAttribute(name = "id", required = true)
    private int id;

    @XmlAttribute(name = "name", required = true)
    private String name;

    public RepositoryLevel() {}

    public RepositoryLevel(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
