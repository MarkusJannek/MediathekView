/*
 * MediathekView
 * Copyright (C) 2008 W. Xaver
 * W.Xaver[at]googlemail.com
 * http://zdfmediathk.sourceforge.net/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package mediathek.tool;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.util.Collections;
import java.util.LinkedList;
import mediathek.daten.DDaten;
import mediathek.daten.Daten;
import mediathek.daten.DatenFilm;

public class GuiFunktionen extends Funktionen {


    public static void copyToClipboard(String s) {
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(s), null);
    }

    public static void setProxy() {
        if (Boolean.parseBoolean(DDaten.system[Konstanten.SYSTEM_HTTP_PROXY_ON_NR])) {
            System.setProperty("proxySet", "true");
            System.setProperty("http.proxyHost", DDaten.system[Konstanten.SYSTEM_HTTP_PROXY_SERVER_NR]);
            System.setProperty("http.proxyPort", DDaten.system[Konstanten.SYSTEM_HTTP_PROXY_PORT_NR]);
            System.setProperty("http.proxyUser", DDaten.system[Konstanten.SYSTEM_HTTP_PROXY_USER_NR]);
            System.setProperty("http.proxyPassword", DDaten.system[Konstanten.SYSTEM_HTTP_PROXY_PWD_NR]);
        } else {
            System.setProperty("proxySet", "false");
        }
    }

    public static String replaceString(String s, DatenFilm film) {
        s = s.replace("%D", film.arr[DatenFilm.FILM_DATUM_NR].equals("") ? DatumZeit.getHeute_yyyyMMdd() : datumDatumZeitReinigen(datumDrehen(film.arr[DatenFilm.FILM_DATUM_NR])));
        s = s.replace("%d", film.arr[DatenFilm.FILM_ZEIT_NR].equals("") ? DatumZeit.getJetzt_HHMMSS() : datumDatumZeitReinigen(film.arr[DatenFilm.FILM_ZEIT_NR]));
        s = s.replace("%t", film.arr[DatenFilm.FILM_THEMA_NR]);
        s = s.replace("%T", film.arr[DatenFilm.FILM_TITEL_NR]);
        s = s.replace("%s", film.arr[DatenFilm.FILM_SENDER_NR]);
        s = s.replace("%H", DatumZeit.getHeute_yyyyMMdd());
        s = s.replace("%h", DatumZeit.getJetzt_HHMMSS());
        s = s.replace("%N", GuiFunktionen.getDateiName(film.arr[DatenFilm.FILM_URL_NR]));
        return s;
    }

    private static String datumDrehen(String datum) {
        String ret = "";
        if (!datum.equals("")) {
            try {
                if (datum.length() == 10) {
                    String tmp = datum.substring(6); // Jahr
                    tmp += "." + datum.substring(3, 5); // Monat
                    tmp += "." + datum.substring(0, 2); // Tag
                    ret = tmp;
                }
            } catch (Exception ex) {
                Log.fehlerMeldung(775421006, Log.FEHLER_ART_PROG, "DatenFilm.datumDrehen", ex, datum);
            }

        }
        return ret;
    }

    private static String datumDatumZeitReinigen(String datum) {
        String ret;
        ret = datum;
        ret = ret.replace(":", "");
        ret = ret.replace(".", "");
        return ret;
    }

    public static String replaceLeerDateiname(String pfad, boolean pfadtrennerEntfernen, boolean leerEntfernen) {
        // verbotene Zeichen entfernen
        // < > ? " : | \ / *
        String ret = pfad;
        boolean winPfad = false;
        if (pfad.length() >= 2) {
            if (pfad.charAt(1) == ':') {
                // damit auch "d:" als Pfad geht
                winPfad = true;
            }
//            if (pfad.substring(1, 3).equals(":\\")) {
//                winPfad = true;
//            }
        }
        if (pfadtrennerEntfernen) {
            ret = ret.replace("\\", "-");
            ret = ret.replace("/", "-");
        } else {
            String sl;
            if (File.separator.equals("\\")) {
                sl = "/";
            } else {
                sl = "\\";
            }
            ret = ret.replace(sl, "-");
        }
        if (leerEntfernen) {
            ret = ret.replace(" ", "_");
        }
        ret = ret.replace("\n", "_");
        ret = ret.replace("\"", "_");
//        ret = ret.replace(",", "_");
//        ret = ret.replace(";", "_");
//        ret = ret.replace("(", "_");
//        ret = ret.replace(")", "_");
        ret = ret.replace("*", "_");
        ret = ret.replace("?", "_");
        ret = ret.replace("<", "_");
        ret = ret.replace(">", "_");
        ret = ret.replace(":", "_");
        ret = ret.replace("'", "_");
        ret = ret.replace("|", "_");
        ret = getAscii(ret);
        if (winPfad) {
            if (ret.length() >= 3) {
                ret = ret.substring(0, 1) + ":" + ret.substring(2);
            } else if (ret.length() >= 2) {
                ret = ret.substring(0, 1) + ":";
            }
        }
        return ret;
    }

    private static String getAscii(String ret) {
        String r = "";
        if (Boolean.parseBoolean(Daten.system[Konstanten.SYSTEM_UNICODE_AENDERN_NR])) {
            return cleanUnicode(ret, "_");
        }
        if (!Boolean.parseBoolean(Daten.system[Konstanten.SYSTEM_NUR_ASCII_NR])) {
            return ret;
        } else {
            char c;
            ret = ret.replace("ä", "ae");
            ret = ret.replace("ö", "oe");
            ret = ret.replace("ü", "ue");
            ret = ret.replace("Ä", "Ae");
            ret = ret.replace("Ö", "Oe");
            ret = ret.replace("Ü", "Ue");
            for (int i = 0; i < ret.length(); ++i) {
                if ((c = ret.charAt(i)) < 127) {
                    r += c;
                } else {
                    r += "_";
                }
            }
        }
        return r;
    }

    public static String cleanUnicode(String ret, String sonst) {
        String r = "";
        char c;
        for (int i = 0; i < ret.length(); ++i) {
            c = ret.charAt(i);
            char hex = ret.charAt(i);
            if (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.BASIC_LATIN) {
                r += c;
            } else // Umlaute, 
            if (c == 'Ä' || c == 'Ö' || c == 'Ü'
                    || c == 'ä' || c == 'ö' || c == 'ü') {
                r += c;
            } else if (c == 'ß') {
                r += "ß";
            } else // Buchstaben
            if (c == 'Â' || c == 'À' || c == 'Å' || c == 'Á') {
                r += "A";
            } else if (c == 'å' || c == 'á' || c == 'à' || c == 'â') {
                r += "a";
            } else if (c == 'Č' || c == 'Č') {
                r += "C";
            } else if (c == 'ć' || c == 'č' || c == 'ç') {
                r += "c";
            } else if (c == 'Đ') {
                r += "D";
            } else if (c == 'É' || c == 'È') {
                r += "E";
            } else if (c == 'é' || c == 'è' || c == 'ê' || c == 'ě' || c == 'ë') {
                r += "e";
            } else if (c == 'í') {
                r += "i";
            } else if (c == 'ñ') {
                r += "n";
            } else if (c == 'ó' || c == 'ô' || c == 'ø') {
                r += "o";
            } else if (c == 'Š') {
                r += "S";
            } else if (c == 'ś' || c == 'š' || c == 'ş') {
                r += "s";
            } else if (c == 'ł' || c == 'Ł') {
                r += "t";
            } else if (c == 'û' || c == 'ù') {
                r += "u";
            } else if (c == 'ý') {
                r += "y";
            } else if (c == 'Ž' || c == 'Ź') {
                r += "Z";
            } else if (c == 'ž' || c == 'ź') {
                r += "z";
            } else if (c == 'æ') {
                r += "ae";
            } else // Rest
            if (c == '\n') {
            } else if (c == '–') {
                r += "-";
            } else if (c == '„') {
                r += "\"";
            } else if (c == '„' || c == '”' || c == '“' || c == '«' || c == '»') {
                r += "\"";
            } else if (c == '?') {
                r += "?";
            } else if (c == '°' || c == '™') {
                r += "";
            } else if (c == '…') {
                r += "...";
            } else if (c == '€') {
                r += "€";
            } else if (c == '´' || c == '’' || c == '‘' || c == '¿') {
                r += "'";
            } else if (c == '\u003F') {
                r += "?";
            } else if (c == '\u0096') {
                r += "-";
            } else if (c == '\u0085') {
            } else if (c == '\u0080') {
            } else if (c == '\u0084') {
            } else if (c == '\u0092') {
            } else if (c == '\u0093') {
            } else if (c == '\u0091') {
                r += "-";
            } else {
                r += sonst;
            }
        }
        return r;
    }

    public static String addsPfad(String pfad1, String pfad2) {
        String ret = "";
        if (pfad1 != null && pfad2 != null) {
            if (!pfad1.equals("") && !pfad2.equals("")) {
                if (pfad1.endsWith(File.separator)) {
                    ret = pfad1.substring(0, pfad1.length() - 1);
                } else {
                    ret = pfad1;
                }
                if (pfad2.charAt(0) == File.separatorChar) {
                    ret += pfad2;
                } else {
                    ret += File.separator + pfad2;
                }
            }
        }
        if (ret.equals("")) {
            Log.fehlerMeldung(283946015, Log.FEHLER_ART_PROG, "GuiFunktionen.addsPfad", pfad1 + " - " + pfad2);
        }
        return ret;
    }

    public static String addUrl(String u1, String u2) {
        if (u1.endsWith("/")) {
            return u1 + u2;
        } else {
            return u1 + "/" + u2;
        }
    }

    public static boolean istUrl(String dateiUrl) {
        return dateiUrl.startsWith("http") ? true : false || dateiUrl.startsWith("www") ? true : false;
    }

    public static String getDateiName(String pfad) {
        //Dateinamen einer URL extrahieren
        String ret = "";
        if (pfad != null) {
            if (!pfad.equals("")) {
                ret = pfad.substring(pfad.lastIndexOf("/") + 1);
            }
        }
        if (ret.contains("?")) {
            ret = ret.substring(0, ret.indexOf("?"));
        }
        if (ret.contains("&")) {
            ret = ret.substring(0, ret.indexOf("&"));
        }
        if (ret.equals("")) {
            Log.fehlerMeldung(395019631, Log.FEHLER_ART_PROG, "GuiFunktionen.getDateiName", pfad);
        }
        return ret;
    }

    public static void listeSort(LinkedList<String> liste) {
        //Stringliste alphabetisch sortieren
        GermanStringSorter sorter = GermanStringSorter.getInstance();
        Collections.sort(liste, sorter);
    }

    public static String getHomePath() {
        //lifert den Pfad zum Homeverzeichnis
        return System.getProperty("user.home");
    }

    public static String getStandardDownloadPath() {
        //lifert den Standardpfad für Downloads
        if (getOs() == OS_MAC) {
            return addsPfad(getHomePath(), "Desktop");
        }
        return addsPfad(getHomePath(), Konstanten.VERZEICNHISS_DOWNLOADS);
    }

    public static String[] addLeerListe(String[] str) {
        //ein Leerzeichen der Liste voranstellen
        int len = str.length + 1;
        String[] liste = new String[len];
        liste[0] = "";
        System.arraycopy(str, 0, liste, 1, len - 1);
        return liste;
    }

    public static String textLaenge(int max, String text, boolean mitte, boolean addVorne) {
        if (text.length() > max) {
            if (mitte) {
                text = text.substring(0, 25) + " .... " + text.substring(text.length() - (max - 31));
            } else {
                text = text.substring(0, max - 1);
            }
        }
        while (text.length() < max) {
            if (addVorne) {
                text = " " + text;
            } else {
                text = text + " ";
            }
        }
        return text;
    }

    public static int getImportArtFilme() {
        int ret;
        try {
            ret = Integer.parseInt(DDaten.system[Konstanten.SYSTEM_IMPORT_ART_FILME_NR]);
        } catch (Exception ex) {
            Daten.system[Konstanten.SYSTEM_IMPORT_ART_FILME_NR] = String.valueOf(GuiKonstanten.UPDATE_FILME_AUTO);
            ret = GuiKonstanten.UPDATE_FILME_AUTO;
        }
        return ret;
    }
}
