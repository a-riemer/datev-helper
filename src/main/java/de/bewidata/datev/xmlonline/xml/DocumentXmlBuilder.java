package de.bewidata.datev.xmlonline.xml;

import de.bewidata.datev.xmlonline.DATEVXmlConfig;
import de.bewidata.datev.xmlonline.jaxb.Archive;
import de.bewidata.datev.xmlonline.jaxb.Content;
import de.bewidata.datev.xmlonline.jaxb.Document;
import de.bewidata.datev.xmlonline.jaxb.FileExtension;
import de.bewidata.datev.xmlonline.jaxb.Header;
import de.bewidata.datev.xmlonline.model.BookingLineWithDocuments;
import de.bewidata.datev.xmlonline.model.SourceDocument;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;

import java.io.OutputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Builds the {@code document.xml} for a DATEV document package (ZIP archive).
 *
 * <p>For each booking line that has at least one source document, a {@code <document>}
 * element is created. The GUID is generated here and simultaneously written into
 * the booking line's {@code Beleglink} field as {@code "BEDI" + guid}.
 *
 * <p><b>Side effect:</b> {@link de.bewidata.datev.xmlonline.model.BookingLine#setDocumentLink}
 * is called for every line with documents. This must happen before the CSV is written.
 */
public class DocumentXmlBuilder {

    private static final JAXBContext CONTEXT;

    static {
        try {
            CONTEXT = JAXBContext.newInstance(Archive.class);
        } catch (JAXBException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private final DATEVXmlConfig config;

    public DocumentXmlBuilder(DATEVXmlConfig config) {
        this.config = config;
    }

    /**
     * Builds the document.xml and writes it to {@code output}.
     * Sets {@code Beleglink} on each booking line as a side effect.
     */
    public void build(List<BookingLineWithDocuments> lines, OutputStream output)
            throws JAXBException {

        Archive archive = new Archive();
        archive.setGuid(generateGuid());
        archive.setGeneratingSystem("DATEV-helper");

        Header header = new Header();
        header.setDate(LocalDateTime.now());
        header.setDescription(config.getDescription());
        header.setConsultantNumber(config.getConsultantNumber());
        header.setClientNumber(config.getClientNumber());
        header.setClientName(config.getClientName());
        archive.setHeader(header);

        Content content = new Content();
        archive.setContent(content);

        for (BookingLineWithDocuments lwd : lines) {
            if (!lwd.hasDocuments()) continue;

            String guid = lwd.getExternalGuid().orElseGet(this::generateGuid);
            lwd.line().setDocumentLink("BEDI \"" + guid + "\"");

            Document doc = new Document();
            doc.setGuid(guid);
            doc.setType(documentTypeOfFirst(lwd.documents()));
            doc.setProcessID(processIdOfFirst(lwd.documents()));

            String desc = lwd.documents().stream()
                    .map(SourceDocument::getDescription)
                    .filter(s -> s != null && !s.isBlank())
                    .findFirst().orElse(null);
            if (desc != null && desc.length() > 40) desc = desc.substring(0, 40);
            doc.setDescription(desc);

            for (SourceDocument sd : lwd.documents()) {
                doc.getExtensions().add(new FileExtension(sd.getFileName()));
            }

            content.getDocuments().add(doc);
        }

        Marshaller marshaller = CONTEXT.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
        marshaller.setProperty(Marshaller.JAXB_SCHEMA_LOCATION,
                "http://xml.datev.de/bedi/tps/document/v06.0 Document_v060.xsd");
        marshaller.marshal(archive, output);
    }

    private String generateGuid() {
        return UUID.randomUUID().toString().toUpperCase();
    }

    private Byte documentTypeOfFirst(List<SourceDocument> docs) {
        if (docs.isEmpty()) return null;
        byte t = docs.get(0).getDocumentType();
        return (t == SourceDocument.DOCUMENT_TYPE_INCOMING
                || t == SourceDocument.DOCUMENT_TYPE_OUTGOING) ? t : null;
    }

    private Byte processIdOfFirst(List<SourceDocument> docs) {
        return docs.isEmpty() ? null : docs.get(0).getProcessId();
    }
}
