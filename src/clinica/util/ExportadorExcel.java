package clinica.util;

import clinica.modelo.Celda;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Escritor de archivos XLSX (Excel) sin dependencias externas.
 * Genera un libro de una sola hoja a partir de una lista de filas de
 * {@link Celda}: la primera fila se exporta como encabezado (en negrita),
 * los montos como numeros con formato de moneda (S/) y los identificadores
 * y conteos como numeros reales.
 * El archivo es un ZIP valido que Excel 2007+ abre sin advertencias.
 */
public final class ExportadorExcel {

    private ExportadorExcel() {
    }

    public static void exportar(List<String[]> filas, Path destino, String nombreHoja) throws IOException {
        List<List<Celda>> celdas = new ArrayList<>();
        for (String[] fila : filas) {
            List<Celda> filaCeldas = new ArrayList<>();
            for (String valor : fila) {
                filaCeldas.add(Celda.texto(valor));
            }
            celdas.add(filaCeldas);
        }
        exportarCeldas(celdas, destino, nombreHoja);
    }

    public static void exportarCeldas(List<List<Celda>> filas, Path destino, String nombreHoja) throws IOException {
        try {
            String hoja = normalizarNombreHoja(nombreHoja);
            try (OutputStream out = Files.newOutputStream(destino)) {
                try (ZipOutputStream zip = new ZipOutputStream(out)) {
                    escribir(zip, "[Content_Types].xml", contentTypes());
                    escribir(zip, "_rels/.rels", relsRaiz());
                    escribir(zip, "docProps/app.xml", appProps());
                    escribir(zip, "docProps/core.xml", coreProps());
                    escribir(zip, "xl/workbook.xml", workbook(hoja));
                    escribir(zip, "xl/_rels/workbook.xml.rels", relsWorkbook());
                    escribir(zip, "xl/styles.xml", estilos());
                    escribir(zip, "xl/worksheets/sheet1.xml", hojaXml(filas));
                }
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

    private static void escribir(ZipOutputStream zip, String entrada, String contenido) throws IOException {
        zip.putNextEntry(new ZipEntry(entrada));
        zip.write(contenido.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private static String normalizarNombreHoja(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            return "Reporte";
        }
        String limpio = nombre.replaceAll("[\\[\\]*?:/\\\\]", " ").trim();
        if (limpio.length() > 31) {
            limpio = limpio.substring(0, 31);
        }
        return limpio.isEmpty() ? "Reporte" : limpio;
    }

    private static String hojaXml(List<List<Celda>> filas) {
        StringBuilder xml = new StringBuilder(4096);
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n");
        xml.append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">");
        int columnas = 0;
        for (List<Celda> fila : filas) {
            columnas = Math.max(columnas, fila.size());
        }
        appendCols(xml, filas, columnas);
        xml.append("<sheetData>");
        int r = 1;
        for (List<Celda> fila : filas) {
            xml.append("<row r=\"").append(r).append('"');
            if (r == 1) {
                xml.append(" ht=\"18\" customHeight=\"1\"");
            }
            xml.append('>');
            for (int c = 0; c < columnas; c++) {
                Celda celda = (c < fila.size()) ? fila.get(c) : null;
                String ref = columnaLetra(c + 1) + r;
                if (celda != null && r > 1 && celda.esNumero()) {
                    int estilo = celda.getTipo() == Celda.Tipo.MONEDA ? 3 : 0;
                    xml.append("<c r=\"").append(ref)
                            .append("\" s=\"").append(estilo).append("\">");
                    xml.append("<v>").append(celda.getTexto()).append("</v>");
                    xml.append("</c>");
                } else {
                    int estilo = (r == 1) ? 1 : 0;
                    String valor = celda == null ? "" : celda.getTexto();
                    xml.append("<c r=\"").append(ref)
                            .append("\" t=\"inlineStr\" s=\"").append(estilo).append("\">");
                    xml.append("<is><t xml:space=\"preserve\">")
                            .append(escapar(valor))
                            .append("</t></is>");
                    xml.append("</c>");
                }
            }
            xml.append("</row>");
            r++;
        }
        xml.append("</sheetData></worksheet>");
        return xml.toString();
    }

    private static void appendCols(StringBuilder xml, List<List<Celda>> filas, int columnas) {
        int[] anchos = new int[columnas];
        for (List<Celda> fila : filas) {
            for (int c = 0; c < columnas && c < fila.size(); c++) {
                Celda celda = fila.get(c);
                int largo = celda == null ? 0 : celda.getTexto().length();
                anchos[c] = Math.max(anchos[c], largo);
            }
        }
        xml.append("<cols>");
        for (int c = 0; c < columnas; c++) {
            int ancho = Math.min(Math.max(anchos[c] + 2, 8), 50);
            xml.append("<col min=\"").append(c + 1).append("\" max=\"").append(c + 1)
                    .append("\" width=\"").append(ancho).append("\" customWidth=\"1\"/>");
        }
        xml.append("</cols>");
    }

    private static String columnaLetra(int indice) {
        StringBuilder letra = new StringBuilder();
        while (indice > 0) {
            int resto = (indice - 1) % 26;
            letra.insert(0, (char) ('A' + resto));
            indice = (indice - 1) / 26;
        }
        return letra.toString();
    }

    private static String escapar(String valor) {
        return sanitizar(valor).replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private static String sanitizar(String valor) {
        StringBuilder limpio = new StringBuilder(valor.length());
        for (int i = 0; i < valor.length(); i++) {
            char caracter = valor.charAt(i);
            if (caracter == '\t' || caracter == '\n' || caracter == '\r' || caracter >= 0x20) {
                limpio.append(caracter);
            } else {
                limpio.append(' ');
            }
        }
        return limpio.toString();
    }

    private static String contentTypes() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
                + "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
                + "<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
                + "<Override PartName=\"/docProps/core.xml\" ContentType=\"application/vnd.openxmlformats-package.core-properties+xml\"/>"
                + "<Override PartName=\"/docProps/app.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.extended-properties+xml\"/>"
                + "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>"
                + "<Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"
                + "<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>"
                + "</Types>";
    }

    private static String relsRaiz() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>"
                + "<Relationship Id=\"rId2\" Type=\"http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties\" Target=\"docProps/core.xml\"/>"
                + "<Relationship Id=\"rId3\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/extended-properties\" Target=\"docProps/app.xml\"/>"
                + "</Relationships>";
    }

    private static String relsWorkbook() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/>"
                + "<Relationship Id=\"rId2\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/>"
                + "</Relationships>";
    }

    private static String workbook(String hoja) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                + "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" "
                + "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">"
                + "<fileVersion appName=\"xl\" lastEdited=\"5\" lowestEdited=\"5\" rupBuild=\"9303\"/>"
                + "<sheets><sheet name=\"" + escapar(hoja)
                + "\" sheetId=\"1\" r:id=\"rId1\"/></sheets>"
                + "</workbook>";
    }

    private static String estilos() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                + "<styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">"
                + "<numFmts count=\"2\">"
                + "<numFmt numFmtId=\"164\" formatCode=\"#,##0.00\"/>"
                + "<numFmt numFmtId=\"165\" formatCode=\"\\\"S/ \\\" #,##0.00\"/>"
                + "</numFmts>"
                + "<fonts count=\"2\">"
                + "<font><sz val=\"11\"/><color rgb=\"FF000000\"/><name val=\"Calibri\"/></font>"
                + "<font><b/><sz val=\"11\"/><color rgb=\"FFFFFFFF\"/><name val=\"Calibri\"/></font>"
                + "</fonts>"
                + "<fills count=\"3\">"
                + "<fill><patternFill patternType=\"none\"/></fill>"
                + "<fill><patternFill patternType=\"gray125\"/></fill>"
                + "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"FF4472C4\"/><bgColor indexed=\"64\"/></patternFill></fill>"
                + "</fills>"
                + "<borders count=\"1\">"
                + "<border><left/><right/><top/><bottom/><diagonal/></border>"
                + "</borders>"
                + "<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>"
                + "<cellXfs count=\"4\">"
                + "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/>"
                + "<xf numFmtId=\"0\" fontId=\"1\" fillId=\"2\" borderId=\"0\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\" applyAlignment=\"1\"><alignment horizontal=\"center\"/></xf>"
                + "<xf numFmtId=\"164\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyNumberFormat=\"1\"/>"
                + "<xf numFmtId=\"165\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyNumberFormat=\"1\"/>"
                + "</cellXfs>"
                + "<cellStyles count=\"1\"><cellStyle name=\"Normal\" xfId=\"0\" builtinId=\"0\"/></cellStyles>"
                + "</styleSheet>";
    }

    private static String appProps() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                + "<Properties xmlns=\"http://schemas.openxmlformats.org/officeDocument/2006/extended-properties\" "
                + "xmlns:vt=\"http://schemas.openxmlformats.org/officeDocument/2006/docPropsVTypes\">"
                + "<Application>Sistema de Gestion para una Clinica</Application>"
                + "</Properties>";
    }

    private static String coreProps() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                + "<cp:coreProperties xmlns:cp=\"http://schemas.openxmlformats.org/package/2006/metadata/core-properties\" "
                + "xmlns:dc=\"http://purl.org/dc/elements/1.1/\" "
                + "xmlns:dcterms=\"http://purl.org/dc/terms/\" "
                + "xmlns:dcmitype=\"http://purl.org/dc/dcmitype/\" "
                + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">"
                + "<dc:creator>Sistema de Gestion para una Clinica</dc:creator>"
                + "<cp:lastModifiedBy>Sistema de Gestion para una Clinica</cp:lastModifiedBy>"
                + "</cp:coreProperties>";
    }
}