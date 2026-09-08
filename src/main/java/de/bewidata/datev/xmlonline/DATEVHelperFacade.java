package de.bewidata.datev.xmlonline;

import de.bewidata.datev.xmlonline.provider.FileByGUIDProvider;
import de.bewidata.datev.xmlonline.split.ByDocumentTypeSplitStrategy;
import de.bewidata.datev.xmlonline.split.SequentialSplitStrategy;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Gateway-freundliche Fassade für den Aufruf aus InterSystems IRIS via Java External Server.
 *
 * <p>Alle Parameter sind einfache Typen (String, long, boolean). Kein Builder, keine
 * statischen Factory-Methoden — für ObjectScript direkt instanziierbar und aufrufbar.
 * Die Methodennamen entsprechen 1:1 denen der ObjectScript-Klasse {@code MP.DATEVHelper}.
 *
 * <p>ObjectScript-Beispiel:
 * <pre>
 * Set gw     = $system.external.getJavaGateway()
 * Do gw.addToPath("datev-helper-cli.jar")
 * Set facade = gw.new("de.bewidata.datev.xmlonline.DATEVHelperFacade")
 * Do facade.setDocumentType("eingang")
 * Set zips   = facade.exportWithFilesByGUID(csvPath, docDir, outputDir)
 * </pre>
 */
public class DATEVHelperFacade {

    private Long    consultantNumber;
    private Long    clientNumber;
    private String  clientName;
    private String  description;
    private String  documentType = "eingang";
    private boolean sequential   = false;

    // ---- Version ----

    /** Returns the datev-helper library version (e.g. {@code "1.0-SNAPSHOT"}). */
    public String getVersion() {
        return DATEVHelperVersion.getVersion();
    }

    // ---- Konfiguration ----

    public void setConsultantNumber(long n)   { this.consultantNumber = n; }
    public void setClientNumber(long n)       { this.clientNumber = n; }
    public void setClientName(String name)    { this.clientName = name; }
    public void setDescription(String desc)   { this.description = desc; }

    /** "eingang" = Rechnungseingang (Standard), "ausgang" = Rechnungsausgang */
    public void setDocumentType(String type)  { this.documentType = type; }

    /** true = alle Zeilen in einem ZIP, false = Trennung nach Belegkreis (Standard) */
    public void setSequential(boolean seq)    { this.sequential = seq; }

    // ---- Verarbeitung ----

    /**
     * Verarbeitet einen EXTF-Buchungsstapel und sucht Belege per GUID im angegebenen
     * Verzeichnis. Dateinamen müssen das Schema {@code <GUID>.<beliebige-Endung>} haben.
     *
     * <p>Beraternummer, Mandantennummer und Beschreibung werden aus der EXTF-CSV gelesen,
     * sofern sie nicht explizit über die Setter gesetzt wurden.
     *
     * @param csvPath     absoluter Pfad zur EXTF-CSV-Datei
     * @param documentDir absoluter Pfad zum Verzeichnis mit den Belegdateien
     * @param outputDir   absoluter Pfad zum Ausgabeverzeichnis für die ZIP-Archive
     * @return ArrayList mit den absoluten Pfaden der erzeugten ZIP-Dateien
     * @throws Exception bei I/O-Fehlern, fehlender Konfiguration oder leerem Verzeichnis
     */
    public ArrayList<String> exportWithFilesByGUID(String csvPath,
                                                   String documentDir,
                                                   String outputDir) throws Exception {
        DATEVXmlConfig.Builder configBuilder = DATEVXmlConfig.builder();
        if (consultantNumber != null) configBuilder.consultantNumber(consultantNumber);
        if (clientNumber     != null) configBuilder.clientNumber(clientNumber);
        if (clientName       != null) configBuilder.clientName(clientName);
        if (description      != null) configBuilder.description(description);
        DATEVXmlConfig config = configBuilder.build();

        FileByGUIDProvider provider = new FileByGUIDProvider();
        provider.configure(Map.of("directory", documentDir, "type", documentType));

        DATEVXmlProcessor processor = new DATEVXmlProcessor(
            config, provider,
            sequential ? new SequentialSplitStrategy() : new ByDocumentTypeSplitStrategy()
        );

        List<Path> zips = processor.process(Path.of(csvPath), Path.of(outputDir));

        ArrayList<String> result = new ArrayList<>(zips.size());
        for (Path zip : zips) {
            result.add(zip.toAbsolutePath().toString());
        }
        return result;
    }
}
