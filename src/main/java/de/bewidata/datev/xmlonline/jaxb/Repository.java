package de.bewidata.datev.xmlonline.jaxb;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

import java.util.ArrayList;
import java.util.List;

/** Ablagestruktur in Belege online (Kategorie / Ordner / Register). */
@XmlAccessorType(XmlAccessType.FIELD)
public class Repository {

    @XmlElement(name = "level", required = true)
    private List<RepositoryLevel> levels = new ArrayList<>();

    public List<RepositoryLevel> getLevels() { return levels; }

    public static Repository of(String kategorie, String ordner, String register) {
        Repository r = new Repository();
        r.levels.add(new RepositoryLevel(1, kategorie));
        r.levels.add(new RepositoryLevel(2, ordner));
        if (register != null) r.levels.add(new RepositoryLevel(3, register));
        return r;
    }
}
