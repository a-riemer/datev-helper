package de.bewidata.datev.xmlonline.jaxb;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Konvertiert LocalDateTime ↔ DATEV-konformes ISO-Format (ohne Zeitzone). */
public class LocalDateTimeAdapter extends XmlAdapter<String, LocalDateTime> {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @Override
    public LocalDateTime unmarshal(String v) {
        return v == null ? null : LocalDateTime.parse(v, FORMATTER);
    }

    @Override
    public String marshal(LocalDateTime v) {
        return v == null ? null : v.format(FORMATTER);
    }
}
