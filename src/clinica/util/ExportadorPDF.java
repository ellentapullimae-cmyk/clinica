package clinica.util;

import clinica.modelo.Celda;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.RGBColor;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Exportador de reportes a PDF (libreria OpenPDF / iText).
 * Genera un documento con titulo, subtitulo, fecha de generacion
 * y una tabla con la informacion del reporte.
 */
public final class ExportadorPDF {

    private static final RGBColor CABECERA = new RGBColor(0x44, 0x72, 0xC4);
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private ExportadorPDF() {
    }

    public static void exportar(String titulo, String subtitulo, List<String[]> filas, Path destino)
            throws IOException {
        List<List<Celda>> celdas = new ArrayList<>();
        for (String[] fila : filas) {
            List<Celda> filaCeldas = new ArrayList<>();
            for (String valor : fila) {
                filaCeldas.add(Celda.texto(valor));
            }
            celdas.add(filaCeldas);
        }
        exportarCeldas(titulo, subtitulo, celdas, destino);
    }

    public static void exportarCeldas(String titulo, String subtitulo, List<List<Celda>> filas, Path destino)
            throws IOException {
        try {
            Document documento = new Document(PageSize.A4.rotate());
            try (OutputStream salida = Files.newOutputStream(destino)) {
                PdfWriter.getInstance(documento, salida);
                documento.open();

                Font fuenteTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, new RGBColor(0x2E, 0x5C, 0x8A));
                Paragraph tituloP = new Paragraph(titulo, fuenteTitulo);
                tituloP.setAlignment(Element.ALIGN_CENTER);
                tituloP.setSpacingAfter(6);
                documento.add(tituloP);

                if (subtitulo != null && !subtitulo.trim().isEmpty()) {
                    Paragraph sub = new Paragraph(subtitulo, FontFactory.getFont(FontFactory.HELVETICA, 11));
                    sub.setAlignment(Element.ALIGN_CENTER);
                    sub.setSpacingAfter(4);
                    documento.add(sub);
                }

                Paragraph generado = new Paragraph("Generado: "
                        + LocalDateTime.now().format(FORMATO_FECHA),
                        FontFactory.getFont(FontFactory.HELVETICA, 9));
                generado.setAlignment(Element.ALIGN_RIGHT);
                generado.setSpacingAfter(12);
                documento.add(generado);

                int columnas = 0;
                for (List<Celda> fila : filas) {
                    columnas = Math.max(columnas, fila.size());
                }
                if (filas.isEmpty() || columnas == 0) {
                    documento.add(new Paragraph("No hay datos para mostrar.",
                            FontFactory.getFont(FontFactory.HELVETICA, 11)));
                    documento.close();
                    return;
                }

                PdfPTable tabla = new PdfPTable(columnas);
                tabla.setWidthPercentage(100);
                tabla.setHeaderRows(1);

                for (List<Celda> fila : filas) {
                    for (int c = 0; c < columnas; c++) {
                        Celda celda = (c < fila.size()) ? fila.get(c) : null;
                        String valor = celda == null ? "" : celda.getTexto();
                        PdfPCell celdaTabla = new PdfPCell(new Phrase(valor,
                                FontFactory.getFont(FontFactory.HELVETICA, 9)));
                        if (fila == filas.get(0)) {
                            celdaTabla.setBackgroundColor(CABECERA);
                            celdaTabla.setPhrase(new Phrase(valor,
                                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, new RGBColor(255, 255, 255))));
                            celdaTabla.setHorizontalAlignment(Element.ALIGN_CENTER);
                        }
                        celdaTabla.setPadding(4);
                        tabla.addCell(celdaTabla);
                    }
                }
                documento.add(tabla);
                documento.close();
            }
        } catch (IOException | RuntimeException e) {
            try {
                Files.deleteIfExists(destino);
            } catch (IOException ignorada) {
                // se conserva el error original
            }
            throw e;
        }
    }
}