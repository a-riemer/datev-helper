package de.bewidata.datev.xmlonline.jaxb;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlSchemaType;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import java.time.LocalDateTime;

/** Metadaten-Header der document.xml. */
@XmlAccessorType(XmlAccessType.FIELD)
public class Header {

    @XmlElement(name = "date", required = true)
    @XmlJavaTypeAdapter(LocalDateTimeAdapter.class)
    @XmlSchemaType(name = "dateTime")
    private LocalDateTime date;

    @XmlElement(name = "description")
    private String description;

    @XmlElement(name = "consultantNumber")
    private Long consultantNumber;

    @XmlElement(name = "clientNumber")
    private Long clientNumber;

    @XmlElement(name = "clientName")
    private String clientName;

    @XmlElement(name = "repository")
    private Repository repository;

    public LocalDateTime getDate() { return date; }
    public void setDate(LocalDateTime date) { this.date = date; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Long getConsultantNumber() { return consultantNumber; }
    public void setConsultantNumber(Long consultantNumber) { this.consultantNumber = consultantNumber; }
    public Long getClientNumber() { return clientNumber; }
    public void setClientNumber(Long clientNumber) { this.clientNumber = clientNumber; }
    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }
    public Repository getRepository() { return repository; }
    public void setRepository(Repository repository) { this.repository = repository; }
}
