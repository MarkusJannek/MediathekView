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
package mediathek.controller.history;

import mSearch.daten.DatenFilm;
import mSearch.daten.ListeFilme;
import mSearch.tool.Log;
import mediathek.config.Daten;
import mediathek.gui.messages.history.HistoryChangedEvent;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.*;

@SuppressWarnings("serial")
public class MVUsedUrls<T extends HistoryChangedEvent> {

    private static final SimpleDateFormat SDF = new SimpleDateFormat("dd.MM.yyyy");
    private final HashSet<String> listeUrls;
    private final LinkedList<MVUsedUrl> listeUrlsSortDate;
    private final String fileName;
    private final String settingsDir;
    private final Class<T> clazz;

    public MVUsedUrls(String fileName, String settingsDir,Class<T> clazz) {
        this.fileName = fileName;
        this.settingsDir = settingsDir;
        this.clazz = clazz;

        listeUrlsSortDate = new LinkedList<>();
        listeUrls = new HashSet<>() {
            @Override
            public void clear() {
                listeUrlsSortDate.clear();
                super.clear();
            }
        };
        listeBauen();
    }

    public synchronized void setGesehen(boolean gesehen, ArrayList<DatenFilm> arrayFilms, ListeFilme listeFilmeHistory) {
        if (arrayFilms.isEmpty()) {
            return;
        }
        if (!gesehen) {
            urlAusLogfileLoeschen(arrayFilms);
            arrayFilms.forEach(listeFilmeHistory::remove);
        } else {
            ArrayList<DatenFilm> neueFilme = new ArrayList<>();
            arrayFilms.stream().filter(film -> !urlPruefen(film.getUrlHistory()))
                    .forEach(film -> {
                        neueFilme.add(film);
                        listeFilmeHistory.add(film);
                    });
            zeileSchreiben(neueFilme);
        }
    }

    private void sendChangeMessage() {
        try {
            final T msg = clazz.getDeclaredConstructor().newInstance();
            Daten.getInstance().getMessageBus().publishAsync(msg);
        } catch (IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    public synchronized void alleLoeschen() {
        listeUrls.clear();
        Path urlPath = getUrlFilePath();
        try {
            Files.deleteIfExists(urlPath);
        } catch (IOException ignored) {
        }

        sendChangeMessage();
    }

    public synchronized boolean urlPruefen(String urlFilm) {
        //wenn url gefunden, dann true zurück
        return listeUrls.contains(urlFilm);
    }

    /**
     * Creates a "model" for the table.
     */
    public synchronized Object[][] getObjectData() {
        int i = 0;
        final Object[][] object = new Object[listeUrlsSortDate.size()][];
        for (var item : listeUrlsSortDate) {
            object[i] = new String[] {item.getDatum(),item.getThema(), item.getTitel(), item.getUrl()};
            ++i;
        }
        return object;
    }

    public synchronized List<MVUsedUrl> getSortedList() {
        ArrayList<MVUsedUrl> ret = new ArrayList<>(listeUrlsSortDate);
        Collections.sort(ret);

        return ret;
    }

    public synchronized void urlAusLogfileLoeschen(String urlFilm) {
        //Logfile einlesen, entsprechende Zeile Filtern und dann Logfile überschreiben
        //wenn die URL im Logfiel ist, dann true zurück
        String zeile;
        boolean gefunden = false;

        final Path urlPath = getUrlFilePath();
        if (Files.notExists(urlPath))
            return;

        final List<String> liste = new ArrayList<>();
        try (InputStream is = Files.newInputStream(urlPath);
             InputStreamReader isr = new InputStreamReader(is);
             LineNumberReader in = new LineNumberReader(isr)) {
            while ((zeile = in.readLine()) != null) {
                if (MVUsedUrl.getUrlAusZeile(zeile).getUrl().equals(urlFilm)) {
                    gefunden = true; //nur dann muss das Logfile auch geschrieben werden
                } else {
                    liste.add(zeile);
                }
            }
        } catch (Exception ex) {
            Log.errorLog(281006874, ex);
        }

        //und jetzt wieder schreiben, wenn nötig
        if (gefunden) {
            try (OutputStream os = Files.newOutputStream(getUrlFilePath());
                 OutputStreamWriter osw = new OutputStreamWriter(os);
                 BufferedWriter bufferedWriter = new BufferedWriter(osw)) {
                for (String entry : liste)
                    bufferedWriter.write(entry + '\n');
            } catch (Exception ex) {
                Log.errorLog(566277080, ex);
            }
        }
        listeUrls.clear();
        listeBauen();

        sendChangeMessage();
    }

    public synchronized void urlAusLogfileLoeschen(ArrayList<DatenFilm> filme) {
        //Logfile einlesen, entsprechende Zeile Filtern und dann Logfile überschreiben
        //wenn die URL im Logfiel ist, dann true zurück
        String zeile;
        boolean gefunden = false, gef;

        final Path urlPath = getUrlFilePath();
        if (Files.notExists(urlPath))
            return;

        List<String> newListe = new ArrayList<>();
        try (InputStream is = Files.newInputStream(urlPath);
             InputStreamReader isr = new InputStreamReader(is);
             LineNumberReader in = new LineNumberReader(isr)) {
            while ((zeile = in.readLine()) != null) {
                gef = false;
                String url = MVUsedUrl.getUrlAusZeile(zeile).getUrl();

                for (DatenFilm film : filme) {
                    if (url.equals(film.getUrlHistory())) {
                        gefunden = true; //nur dann muss das Logfile auch geschrieben werden
                        gef = true; // und die Zeile wird verworfen
                        break;
                    }
                }
                if (!gef) {
                    newListe.add(zeile);
                }

            }
        } catch (Exception ex) {
            Log.errorLog(401020398, ex);
        }

        //und jetzt wieder schreiben, wenn nötig
        if (gefunden) {
            try (OutputStream os = Files.newOutputStream(getUrlFilePath());
                 OutputStreamWriter osw = new OutputStreamWriter(os);
                 BufferedWriter bufferedWriter = new BufferedWriter(osw)) {
                for (String entry : newListe) {
                    bufferedWriter.write(entry + '\n');
                }
            } catch (Exception ex) {
                Log.errorLog(784512067, ex);
            }
        }
        listeUrls.clear();
        listeBauen();

        sendChangeMessage();
    }

    public synchronized void zeileSchreiben(String thema, String titel, String url) {
        String text;
        String datum = SDF.format(new Date());
        listeUrls.add(url);
        listeUrlsSortDate.add(new MVUsedUrl(datum, thema, titel, url));

        try (OutputStream os = Files.newOutputStream(getUrlFilePath(), StandardOpenOption.APPEND);
             OutputStreamWriter osw = new OutputStreamWriter(os);
             BufferedWriter bufferedWriter = new BufferedWriter(osw)) {
            text = MVUsedUrl.getUsedUrl(datum, thema, titel, url);
            bufferedWriter.write(text);
        } catch (Exception ex) {
            Log.errorLog(945258023, ex);
        }

        sendChangeMessage();
    }

    public synchronized void zeileSchreiben(ArrayList<DatenFilm> arrayFilms) {
        String text;
        String datum = SDF.format(new Date());

        try (OutputStream os = Files.newOutputStream(getUrlFilePath(), StandardOpenOption.APPEND);
             OutputStreamWriter osw = new OutputStreamWriter(os);
             BufferedWriter bufferedWriter = new BufferedWriter(osw)) {

            for (DatenFilm film : arrayFilms) {
                // film.arr[DatenFilm.FILM_THEMA_NR], film.arr[DatenFilm.FILM_TITEL_NR], film.getUrlHistory()
                listeUrls.add(film.getUrlHistory());
                listeUrlsSortDate.add(new MVUsedUrl(datum, film.getThema(), film.getTitle(), film.getUrlHistory()));
                text = MVUsedUrl.getUsedUrl(datum, film.getThema(), film.getTitle(), film.getUrlHistory());
                bufferedWriter.write(text);
            }
        } catch (Exception ex) {
            Log.errorLog(420312459, ex);
        }

        sendChangeMessage();
    }

    // eigener Thread!!
    public synchronized void createLineWriterThread(LinkedList<MVUsedUrl> mvuuList) {
        Thread t = new LineWriterThread(mvuuList);
        t.start();
    }

    private Path getUrlFilePath() {
        Path urlPath = null;
        try {
            urlPath = Paths.get(settingsDir).resolve(fileName);
            if (Files.notExists(urlPath))
                Files.createFile(urlPath);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return urlPath;
    }

    private void listeBauen() {
        //LinkedList mit den URLs aus dem Logfile bauen
        Path urlPath = getUrlFilePath();

        try (InputStream is = Files.newInputStream(urlPath);
             InputStreamReader isr = new InputStreamReader(is);
             LineNumberReader in = new LineNumberReader(isr)) {
            String zeile;
            while ((zeile = in.readLine()) != null) {
                MVUsedUrl mvuu = MVUsedUrl.getUrlAusZeile(zeile);
                listeUrls.add(mvuu.getUrl());
                listeUrlsSortDate.add(mvuu);
            }
        } catch (Exception ex) {
            Log.errorLog(926362547, ex);
        }
    }

    class LineWriterThread extends Thread {

        private final LinkedList<MVUsedUrl> mvuuList;

        public LineWriterThread(LinkedList<MVUsedUrl> mvuuList) {
            this.mvuuList = mvuuList;
            setName(LineWriterThread.class.getName());
        }

        @Override
        public synchronized void run() {
            zeilenSchreiben();
        }

        private synchronized void zeilenSchreiben() {
            String text;
            try (OutputStream os = Files.newOutputStream(getUrlFilePath(), StandardOpenOption.APPEND);
                 OutputStreamWriter osw = new OutputStreamWriter(os);
                 BufferedWriter bufferedWriter = new BufferedWriter(osw)) {
                for (MVUsedUrl mvuu : mvuuList) {
                    listeUrls.add(mvuu.getUrl());
                    listeUrlsSortDate.add(mvuu);
                    text = mvuu.getUsedUrl();
                    bufferedWriter.write(text);
                }
            } catch (Exception ex) {
                Log.errorLog(945258023, ex);
            }
            sendChangeMessage();
        }
    }

}
