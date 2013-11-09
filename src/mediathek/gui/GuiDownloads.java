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
package mediathek.gui;

import com.jidesoft.utils.SystemInfo;
import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import mediathek.MVStatusBar_Mac;
import mediathek.MediathekGui;
import mediathek.controller.starter.Start;
import mediathek.daten.Daten;
import mediathek.daten.DatenDownload;
import mediathek.daten.DatenPset;
import mediathek.gui.dialog.DialogEditDownload;
import mediathek.gui.dialog.MVFilmInformation;
import mediathek.res.GetIcon;
import mediathek.tool.BeobTableHeader;
import mediathek.tool.CellRendererDownloads;
import mediathek.tool.Datum;
import mediathek.tool.DirOpenAction;
import mediathek.tool.GuiFunktionen;
import mediathek.tool.GuiKonstanten;
import mediathek.tool.HinweisKeineAuswahl;
import mediathek.tool.Konstanten;
import mediathek.tool.ListenerMediathekView;
import mediathek.tool.MVFilmSize;
import mediathek.tool.MVJTable;
import mediathek.tool.MVMessageDialog;
import mediathek.tool.OpenPlayerAction;
import mediathek.tool.TModelDownload;
import msearch.daten.DatenFilm;
import msearch.filmeSuchen.MSearchListenerFilmeLaden;
import msearch.filmeSuchen.MSearchListenerFilmeLadenEvent;

public class GuiDownloads extends PanelVorlage {

    private MVFilmInformation filmInfoHud;
    private PanelBeschreibung panelBeschreibung;
    private int zeileVon = -1;

    public GuiDownloads(Daten d, Frame parentComponent) {
        super(d, parentComponent);
        initComponents();
        tabelle = new MVJTable(MVJTable.TABELLE_TAB_DOWNLOADS);
        jScrollPane1.setViewportView(tabelle);
        filmInfoHud = daten.filmInfoHud;
        panelBeschreibung = new PanelBeschreibung(daten);
        jPanelBeschreibung.setLayout(new BorderLayout());
        jPanelBeschreibung.add(panelBeschreibung, BorderLayout.CENTER);
        init();
        tabelle.initTabelle();
        if (tabelle.getRowCount() > 0) {
            tabelle.setRowSelectionInterval(0, 0);
        }
        addListenerMediathekView();
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                // erst wenn das Programm geladen ist
                downloadsAktualisieren(); // die Tabelle wird dabei gleich geladen
            }
        });
    }
    //===================================
    //public
    //===================================

    @Override
    public void isShown() {
        super.isShown();
        daten.mediathekGui.setToolbar(MediathekGui.UIButtonState.DOWNLOAD);
        daten.mediathekGui.getStatusBar().setIndexForCenterDisplay(MVStatusBar_Mac.StatusbarIndex.DOWNLOAD);
        aktFilmSetzen();
    }

    public void aktualisieren() {
        downloadsAktualisieren();
    }

    public void filmAbspielen() {
        filmAbspielen_();
    }

    public void starten(boolean alle) {
        filmStartenWiederholenStoppen(alle, true /* starten */);
    }

    public void stoppen(boolean alle) {
        filmStartenWiederholenStoppen(alle, false /* starten */);
    }

    public void wartendeStoppen() {
        wartendeDownloadsStoppen();
    }

    public void vorziehen() {
        downloadsVorziehen();
    }

    public void zurueckstellen() {
        downloadLoeschen(false);
    }

    public void loeschen() {
        downloadLoeschen(true);
    }

    public void aufraeumen() {
        downloadsAufraeumen();
    }

    public void aendern() {
        downloadAendern();
    }

    //===================================
    //private
    //===================================
    private void init() {
        panelBeschreibungSetzen();
        jRadioButtonAbos.setForeground(GuiKonstanten.ABO_FOREGROUND);
        jRadioButtonDownloads.setForeground(GuiKonstanten.DOWNLOAD_FOREGROUND);
        tabelle.setDefaultRenderer(Object.class, new CellRendererDownloads());
        tabelle.setDefaultRenderer(Datum.class, new CellRendererDownloads());
        tabelle.setDefaultRenderer(MVFilmSize.class, new CellRendererDownloads());
        tabelle.setModel(new TModelDownload(new Object[][]{}, DatenDownload.COLUMN_NAMES));
        tabelle.addMouseListener(new BeobMausTabelle());
        tabelle.getSelectionModel().addListSelectionListener(new BeobachterTableSelect());
        tabelle.getTableHeader().addMouseListener(new BeobTableHeader(tabelle, DatenDownload.COLUMN_NAMES, DatenDownload.spaltenAnzeigen) {
            @Override
            public void tabelleLaden_() {
                tabelleLaden();
            }
        });
        jRadioButtonAlles.addActionListener(new BeobAnzeige());
        jRadioButtonAbos.addActionListener(new BeobAnzeige());
        jRadioButtonDownloads.addActionListener(new BeobAnzeige());
        ListenerMediathekView.addListener(new ListenerMediathekView(ListenerMediathekView.EREIGNIS_LISTE_DOWNLOADS, GuiDownloads.class.getSimpleName()) {
            @Override
            public void ping() {
                tabelleLaden();
            }
        });
        Daten.filmeLaden.addAdListener(new MSearchListenerFilmeLaden() {
            @Override
            public void fertig(MSearchListenerFilmeLadenEvent event) {
                if (Boolean.parseBoolean(Daten.system[Konstanten.SYSTEM_ABOS_SOFORT_SUCHEN_NR])) {
                    downloadsAktualisieren();
                }
            }
        });
    }

    private void addListenerMediathekView() {
        ListenerMediathekView.addListener(new ListenerMediathekView(ListenerMediathekView.EREIGNIS_ART_DOWNLOAD_PROZENT, GuiDownloads.class.getSimpleName()) {
            @Override
            public void ping() {
                tabelleProzentGeaendert();
            }
        });
        ListenerMediathekView.addListener(new ListenerMediathekView(ListenerMediathekView.EREIGNIS_BLACKLIST_GEAENDERT, GuiDownloads.class.getSimpleName()) {
            @Override
            public void ping() {
                if (Boolean.parseBoolean(Daten.system[Konstanten.SYSTEM_ABOS_SOFORT_SUCHEN_NR])
                        && Boolean.parseBoolean(Daten.system[Konstanten.SYSTEM_BLACKLIST_AUCH_ABO_NR])) {
                    // nur auf Blacklist reagieren, wenn auch für Abos eingeschaltet
                    downloadsAktualisieren();
                }
            }
        });
        ListenerMediathekView.addListener(new ListenerMediathekView(ListenerMediathekView.EREIGNIS_BLACKLIST_AUCH_FUER_ABOS, GuiDownloads.class.getSimpleName()) {
            @Override
            public void ping() {
                if (Boolean.parseBoolean(Daten.system[Konstanten.SYSTEM_ABOS_SOFORT_SUCHEN_NR])) {
                    downloadsAktualisieren();
                }
            }
        });
        ListenerMediathekView.addListener(new ListenerMediathekView(ListenerMediathekView.EREIGNIS_LISTE_ABOS, GuiDownloads.class.getSimpleName()) {
            @Override
            public void ping() {
                if (Boolean.parseBoolean(Daten.system[Konstanten.SYSTEM_ABOS_SOFORT_SUCHEN_NR])) {
                    downloadsAktualisieren();
                }
            }
        });
        ListenerMediathekView.addListener(new ListenerMediathekView(ListenerMediathekView.EREIGNIS_REIHENFOLGE_DOWNLOAD, GuiDownloads.class.getSimpleName()) {
            @Override
            public void ping() {
                tabelleLaden();
            }
        });
        ListenerMediathekView.addListener(new ListenerMediathekView(ListenerMediathekView.EREIGNIS_START_EVENT, GuiDownloads.class.getSimpleName()) {
            @Override
            public void ping() {
                tabelle.fireTableDataChanged(true /*setSpalten*/);
                // aktFilmSetzen();
                setInfo();
            }
        });
        ListenerMediathekView.addListener(new ListenerMediathekView(ListenerMediathekView.EREIGNIS_PANEL_BESCHREIBUNG_ANZEIGEN, GuiDownloads.class.getSimpleName()) {
            @Override
            public void ping() {
                panelBeschreibungSetzen();
            }
        });
    }

    private void panelBeschreibungSetzen() {
        jPanelBeschreibung.setVisible(Boolean.parseBoolean(Daten.system[Konstanten.SYSTEM_PANEL_BESCHREIBUNG_ANZEIGEN_NR]));
    }

    private synchronized void tabelleLaden() {
        // nur Downloads die schon in der Liste sind werden geladen
        boolean abo, download;
        stopBeob = true;
        tabelle.getSpalten();
        if (jRadioButtonAlles.isSelected()) {
            abo = true;
            download = true;
        } else if (jRadioButtonAbos.isSelected()) {
            abo = true;
            download = false;
        } else {
            abo = false;
            download = true;
        }
        Daten.listeDownloads.getModel((TModelDownload) tabelle.getModel(), abo, download);
        tabelle.setSpalten();
        stopBeob = false;
        aktFilmSetzen();
        setInfo();
    }

    private synchronized void downloadsAktualisieren() {
        // erledigte entfernen, nicht gestartete Abos entfernen und neu nach Abos suchen
        downloadsAufraeumen();
        Daten.listeDownloads.zurueckgestellteWiederAktivieren();
        Daten.listeDownloads.abosLoschenWennNochNichtGestartet();
        Daten.listeDownloads.abosSuchen();
        tabelleLaden();
        if (Boolean.parseBoolean(Daten.system[Konstanten.SYSTEM_DOWNLOAD_SOFORT_STARTEN_NR])) {
            // und wenn gewollt auch gleich starten
            filmStartenWiederholenStoppen(true /*alle*/, true /*starten*/);
        }
    }

    private synchronized void downloadsAufraeumen() {
        // abgeschlossene Downloads werden aus der Tabelle/Liste entfernt
        // die Starts dafür werden auch gelöscht
        Daten.listeDownloads.listePutzen();
    }

    private synchronized void downloadsAufraeumen(DatenDownload datenDownload) {
        // abgeschlossene Downloads werden aus der Tabelle/Liste entfernt
        // die Starts dafür werden auch gelöscht
        Daten.listeDownloads.listePutzen(datenDownload);
    }

    private synchronized void downloadAendern() {
        int row = tabelle.getSelectedRow();
        if (row != -1) {
            int delRow = tabelle.convertRowIndexToModel(row);
            String url = tabelle.getModel().getValueAt(delRow, DatenDownload.DOWNLOAD_URL_NR).toString();
            DatenDownload datenDownload = Daten.listeDownloads.getDownloadByUrl(url);
            DatenDownload datenDownloadKopy = datenDownload.getCopy();
            DialogEditDownload dialog = new DialogEditDownload(null, true, datenDownloadKopy);
            dialog.setVisible(true);
            if (dialog.ok) {
                datenDownload.aufMichKopieren(datenDownloadKopy);
                tabelleLaden();
            }
        } else {
            new HinweisKeineAuswahl().zeigen(parentComponent);
        }
    }

    private void downloadsVorziehen() {
        String[] urls;
        // ==========================
        // erst mal die URLs sammeln
        int[] rows = tabelle.getSelectedRows();
        urls = new String[rows.length];
        if (rows.length >= 0) {
            for (int i = 0; i < rows.length; i++) {
                int row = tabelle.convertRowIndexToModel(rows[i]);
                urls[i] = tabelle.getModel().getValueAt(row, DatenDownload.DOWNLOAD_URL_NR).toString();
            }
            Daten.listeDownloads.downloadsVorziehen(urls);
        } else {
            new HinweisKeineAuswahl().zeigen(parentComponent);
        }
    }

    private void zielordnerOeffnen() {
        int row = tabelle.getSelectedRow();
        if (row >= 0) {
            String url = tabelle.getModel().getValueAt(tabelle.convertRowIndexToModel(row), DatenDownload.DOWNLOAD_URL_NR).toString();
            DatenDownload download = Daten.listeDownloads.getDownloadByUrl(url);
            String s = download.arr[DatenDownload.DOWNLOAD_ZIEL_PFAD_NR];
            DirOpenAction.zielordnerOeffnen(parentComponent, s);
        } else {
            new HinweisKeineAuswahl().zeigen(parentComponent);
        }
    }

    private void filmAbspielen_() {
        int row = tabelle.getSelectedRow();
        if (row >= 0) {
            String url = tabelle.getModel().getValueAt(tabelle.convertRowIndexToModel(row), DatenDownload.DOWNLOAD_URL_NR).toString();
            DatenDownload download = Daten.listeDownloads.getDownloadByUrl(url);
            String s = download.arr[DatenDownload.DOWNLOAD_ZIEL_PFAD_DATEINAME_NR];
            OpenPlayerAction.filmAbspielen(parentComponent, s);
        } else {
            new HinweisKeineAuswahl().zeigen(parentComponent);
        }
    }

    private void downloadLoeschen(boolean dauerhaft) {
        int rows[] = tabelle.getSelectedRows();
        if (rows.length > 0) {
            ArrayList<String> arrayUrls = new ArrayList<>();
            ArrayList<String[]> arrayUrlsAbo = new ArrayList<>();
            String url;
            for (int row : rows) {
                url = tabelle.getModel().getValueAt(tabelle.convertRowIndexToModel(row), DatenDownload.DOWNLOAD_URL_NR).toString();
                DatenDownload download = Daten.listeDownloads.getDownloadByUrl(url);
                if (dauerhaft) {
                    arrayUrls.add(url);
                    if (download.istAbo()) {
                        // ein Abo wird zusätzlich ins Logfile geschrieben
                        arrayUrlsAbo.add(new String[]{download.arr[DatenDownload.DOWNLOAD_THEMA_NR],
                            download.arr[DatenDownload.DOWNLOAD_TITEL_NR],
                            download.arr[DatenDownload.DOWNLOAD_FILM_URL_NR]});
                    }
                } else {
                    // wenn nicht dauerhaft
                    download.zurueckstellen();
                }
            }
            if (!arrayUrlsAbo.isEmpty()) {
                daten.erledigteAbos.zeileSchreiben(arrayUrlsAbo);
            }
            Daten.listeDownloads.delDownloadByUrl(arrayUrls, false /*nurStart*/);
            tabelleLaden();
            ersteZeileMarkieren();
        } else {
            new HinweisKeineAuswahl().zeigen(parentComponent);
        }
    }

    private void ersteZeileMarkieren() {
        if (tabelle.getRowCount() > 0) {
            // sonst ist schon eine Zeile markiert
            if (tabelle.getSelectedRow() == -1) {
                tabelle.requestFocus();
                tabelle.setRowSelectionInterval(0, 0);
            }
        }

    }

    private void filmStartenWiederholenStoppen(boolean alle, boolean starten /* starten/wiederstarten oder stoppen */) {
        // bezieht sich immer auf "alle" oder nur die markierten
        // Film der noch keinen Starts hat wird gestartet
        // Film dessen Start schon auf fertig/fehler steht wird wieder gestartet
        // bei !starten wird der Film gestoppt
        String[] urls;
        ArrayList<String> arrayUrls = new ArrayList<>();
        ArrayList<DatenDownload> arrayDownload = new ArrayList<>();
        // ==========================
        // erst mal die Liste nach der Tabelle sortieren
        if (starten && alle) {
            if (tabelle.getRowCount() == 0) {
                return;
            }
            for (int i = 0; i < tabelle.getRowCount(); ++i) {
                DatenDownload datenDownload = (DatenDownload) tabelle.getModel().getValueAt(tabelle.convertRowIndexToModel(i), DatenDownload.DOWNLOAD_REF_NR);
                Daten.listeDownloads.remove(datenDownload);
                Daten.listeDownloads.add(datenDownload);
            }
            Daten.listeDownloads.listeNummerieren();
        }
        // ==========================
        // erst mal die URLs sammeln
        if (alle) {
            urls = new String[tabelle.getRowCount()];
            for (int i = 0; i < tabelle.getRowCount(); ++i) {
                urls[i] = tabelle.getModel().getValueAt(tabelle.convertRowIndexToModel(i), DatenDownload.DOWNLOAD_URL_NR).toString();
            }
        } else {
            int[] rows = tabelle.getSelectedRows();
            urls = new String[rows.length];
            if (rows.length >= 0) {
                for (int i = 0; i < rows.length; i++) {
                    int row = tabelle.convertRowIndexToModel(rows[i]);
                    urls[i] = tabelle.getModel().getValueAt(row, DatenDownload.DOWNLOAD_URL_NR).toString();
                }
            } else {
                new HinweisKeineAuswahl().zeigen(parentComponent);
            }
        }
        if (!starten) {
            // dann das Starten von neuen Downloads etwas Pausieren
            daten.starterClass.pause();
        }
        // ========================
        // und jetzt abarbeiten
        for (String url : urls) {
            DatenDownload download = Daten.listeDownloads.getDownloadByUrl(url);
            if (starten) {
                // ==========================================
                // starten
                if (download.start != null) {
                    if (download.start.status == Start.STATUS_RUN) {
                        // dann läuft er schon
                        continue;
                    }
                    if (download.start.status > Start.STATUS_RUN) {
                        // wenn er noch läuft gibts nix
                        // wenn er schon fertig ist, erst mal fragen vor dem erneuten Starten
                        int a = JOptionPane.showConfirmDialog(parentComponent, "Film nochmal starten?  ==> " + download.arr[DatenDownload.DOWNLOAD_TITEL_NR],
                                "Fertiger Download", JOptionPane.YES_NO_OPTION);
                        if (a != JOptionPane.YES_OPTION) {
                            // weiter mit der nächsten URL
                            continue;
                        }
                        arrayUrls.add(url);
                        if (download.istAbo()) {
                            // wenn er schon feritg ist und ein Abos ist, Url auch aus dem Logfile löschen, der Film ist damit wieder auf "Anfang"
                            daten.erledigteAbos.urlAusLogfileLoeschen(url);
                        }
                    }
                }
                arrayDownload.add(download);
            } else {
                // ==========================================
                // stoppen
                if (download.start != null) {
                    // wenn kein s -> dann gibts auch nichts zum stoppen oder wieder-starten
                    if (download.start.status <= Start.STATUS_RUN) {
                        // löschen -> nur wenn noch läuft, sonst gibts nichts mehr zum löschen
                        arrayUrls.add(url);
                    }
                    arrayDownload.add(download);
                }
            }
        }
        // ========================
        // jetzt noch die Starts stoppen
        Daten.listeDownloads.delDownloadByUrl(arrayUrls, true /*nurStart*/);
        // und die Downloads starten oder stoppen
        if (starten) {
            //alle Downloads starten/wiederstarten
            DatenDownload.startenDownloads(daten, arrayDownload);
        } else {
            //oder stoppen
            ListenerMediathekView.notify(ListenerMediathekView.EREIGNIS_ART_DOWNLOAD_PROZENT, GuiDownloads.class.getName());
        }
        tabelleLaden();
    }

    private void wartendeDownloadsStoppen() {
        // es werden alle noch nicht gestarteten Downloads gelöscht
        ArrayList<String> urls = new ArrayList<>();
        for (int i = 0; i < tabelle.getRowCount(); ++i) {
            int delRow = tabelle.convertRowIndexToModel(i);
            String url = tabelle.getModel().getValueAt(delRow, DatenDownload.DOWNLOAD_URL_NR).toString();
            DatenDownload datenDownload = Daten.listeDownloads.getDownloadByUrl(url);
            if (datenDownload.start != null) {
                if (datenDownload.start.status < Start.STATUS_RUN) {
                    urls.add(url);
                }
            }
        }
        Daten.listeDownloads.delDownloadByUrl(urls, true /*nurStart*/);
    }

    private void tabelleProzentGeaendert() {
        Daten.listeDownloads.setModelProgress((TModelDownload) tabelle.getModel());
        setInfo();
    }

    private void setInfo() {
        String textLinks;
        // Text links: Zeilen Tabelle
        int laufen = Daten.listeDownloads.getStartsRun();
        int warten = Daten.listeDownloads.getStartsNotStarted();
        int gesamt = tabelle.getModel().getRowCount();
        if (gesamt == 1) {
            textLinks = "1 Download,";
        } else {
            textLinks = gesamt + " Downloads,";
        }
        textLinks += " (";
        if (laufen == 1) {
            textLinks += "1 läuft,";
        } else {
            textLinks += laufen + " laufen,";
        }
        if (warten == 1) {
            textLinks += " 1 wartet";
        } else {
            textLinks += " " + warten + " warten";
        }
        textLinks += ")";
        // Infopanel setzen
        daten.mediathekGui.getStatusBar().setTextLeft(MVStatusBar_Mac.StatusbarIndex.DOWNLOAD, textLinks);
    }

    private void aktFilmSetzen() {
        if (this.isShowing()) {
            DatenFilm aktFilm = null;
            int selectedTableRow = tabelle.getSelectedRow();
            if (selectedTableRow >= 0) {
                int selectedModelRow = tabelle.convertRowIndexToModel(selectedTableRow);
                DatenDownload download = Daten.listeDownloads.getDownloadByUrl(tabelle.getModel().getValueAt(selectedModelRow, DatenDownload.DOWNLOAD_URL_NR).toString());
                if (download != null) {
                    // wenn beim Löschen aufgerufen, ist der Download schon weg
                    if (download.film == null) {
                        // geladener Einmaldownload nach Programmstart
                        download.film = Daten.listeFilme.getFilmByUrl_klein_hoch_hd(tabelle.getModel().getValueAt(selectedModelRow, DatenDownload.DOWNLOAD_URL_NR).toString());
                    }
                    if (download.film != null) {
                        aktFilm = download.film;
                    }
                }
            }
            filmInfoHud.updateCurrentFilm(aktFilm);
            // Beschreibung setzen
            panelBeschreibung.setAktFilm(aktFilm);
        }
    }

    /**
     * This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        javax.swing.ButtonGroup buttonGroup1 = new javax.swing.ButtonGroup();
        jScrollPane1 = new javax.swing.JScrollPane();
        javax.swing.JTable jTable1 = new javax.swing.JTable();
        jPanelFilter = new javax.swing.JPanel();
        jRadioButtonAlles = new javax.swing.JRadioButton();
        jRadioButtonDownloads = new javax.swing.JRadioButton();
        jRadioButtonAbos = new javax.swing.JRadioButton();
        jLabel1 = new javax.swing.JLabel();
        jPanelBeschreibung = new javax.swing.JPanel();

        jTable1.setAutoCreateRowSorter(true);
        jTable1.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_OFF);
        jScrollPane1.setViewportView(jTable1);

        jPanelFilter.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153)));

        buttonGroup1.add(jRadioButtonAlles);
        jRadioButtonAlles.setSelected(true);
        jRadioButtonAlles.setText(" alle");
        jRadioButtonAlles.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(204, 204, 204)));
        jRadioButtonAlles.setBorderPainted(true);

        buttonGroup1.add(jRadioButtonDownloads);
        jRadioButtonDownloads.setText(" Downloads ");
        jRadioButtonDownloads.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(204, 204, 204)));
        jRadioButtonDownloads.setBorderPainted(true);

        buttonGroup1.add(jRadioButtonAbos);
        jRadioButtonAbos.setText(" Abos ");
        jRadioButtonAbos.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(204, 204, 204)));
        jRadioButtonAbos.setBorderPainted(true);

        jLabel1.setText("Anzeigen:");

        javax.swing.GroupLayout jPanelFilterLayout = new javax.swing.GroupLayout(jPanelFilter);
        jPanelFilter.setLayout(jPanelFilterLayout);
        jPanelFilterLayout.setHorizontalGroup(
            jPanelFilterLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelFilterLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jRadioButtonAlles)
                .addGap(18, 18, 18)
                .addComponent(jRadioButtonDownloads)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jRadioButtonAbos)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanelFilterLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jRadioButtonAbos, jRadioButtonAlles, jRadioButtonDownloads});

        jPanelFilterLayout.setVerticalGroup(
            jPanelFilterLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelFilterLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelFilterLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(jRadioButtonAlles)
                    .addComponent(jRadioButtonDownloads)
                    .addComponent(jRadioButtonAbos)
                    .addComponent(jLabel1))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanelBeschreibung.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153)));

        javax.swing.GroupLayout jPanelBeschreibungLayout = new javax.swing.GroupLayout(jPanelBeschreibung);
        jPanelBeschreibung.setLayout(jPanelBeschreibungLayout);
        jPanelBeschreibungLayout.setHorizontalGroup(
            jPanelBeschreibungLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 668, Short.MAX_VALUE)
        );
        jPanelBeschreibungLayout.setVerticalGroup(
            jPanelBeschreibungLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 127, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanelFilter, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanelBeschreibung, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.TRAILING))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanelFilter, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 312, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanelBeschreibung, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(6, 6, 6))
        );
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanelBeschreibung;
    private javax.swing.JPanel jPanelFilter;
    private javax.swing.JRadioButton jRadioButtonAbos;
    private javax.swing.JRadioButton jRadioButtonAlles;
    private javax.swing.JRadioButton jRadioButtonDownloads;
    private javax.swing.JScrollPane jScrollPane1;
    // End of variables declaration//GEN-END:variables

    private class BeobachterTableSelect implements ListSelectionListener {

        @Override
        public void valueChanged(ListSelectionEvent event) {
            if (!event.getValueIsAdjusting()) {
                aktFilmSetzen();
            }
        }
    }

    public class BeobMausTabelle extends MouseAdapter {

        private Point p;

        @Override
        public void mouseClicked(MouseEvent arg0) {
            if (arg0.getButton() == MouseEvent.BUTTON1) {
                if (arg0.getClickCount() == 1) {
                    p = arg0.getPoint();
                    int row = tabelle.rowAtPoint(p);
                    int column = tabelle.columnAtPoint(p);
                    if (row >= 0) {
                        buttonTable(row, column);
                    }
                } else if (arg0.getClickCount() > 1) {
                    downloadAendern();
                }
            }
        }

        @Override
        public void mousePressed(MouseEvent arg0) {
            if (arg0.isPopupTrigger()) {
                showMenu(arg0);
            }
        }

        @Override
        public void mouseReleased(MouseEvent arg0) {
            if (arg0.isPopupTrigger()) {
                showMenu(arg0);
            }
        }

        private void buttonTable(int row, int column) {
            if (row != -1) {
                DatenDownload datenDownload = (DatenDownload) tabelle.getModel().getValueAt(tabelle.convertRowIndexToModel(row), DatenDownload.DOWNLOAD_REF_NR);
                if (column == DatenDownload.DOWNLOAD_BUTTON_START_NR) {
                    // filmStartenWiederholenStoppen(boolean alle, boolean starten /* starten/wiederstarten oder stoppen */)
                    if (datenDownload.start != null) {
                        if (datenDownload.start.status >= Start.STATUS_FERTIG) {
                            filmAbspielen_();
                        } else if (datenDownload.start.status < Start.STATUS_RUN) {
                            // Download starten
                            filmStartenWiederholenStoppen(false, true);
                        } else {
                            // Download stoppen
                            filmStartenWiederholenStoppen(false, false);
                        }
                    } else {
                        // Download starten
                        filmStartenWiederholenStoppen(false, true);
                    }
                } else if (column == DatenDownload.DOWNLOAD_BUTTON_DEL_NR) {
                    if (datenDownload.start != null) {
                        if (datenDownload.start.status >= Start.STATUS_FERTIG) {
                            downloadsAufraeumen(datenDownload);
                        } else {
                            // Download dauerhaft löschen
                            downloadLoeschen(true);
                        }
                    } else {
                        // Download dauerhaft löschen
                        downloadLoeschen(true);
                    }
                }
            }
        }

        private void showMenu(MouseEvent evt) {
            p = evt.getPoint();
            int nr = tabelle.rowAtPoint(p);
            if (nr >= 0) {
                tabelle.setRowSelectionInterval(nr, nr);
            }
            JPopupMenu jPopupMenu = new JPopupMenu();

            //Film vorziehen
            int row = tabelle.getSelectedRow();
            boolean wartenOderLaufen = false;
            if (row >= 0) {
                int delRow = tabelle.convertRowIndexToModel(row);
                DatenDownload datenDownload = Daten.listeDownloads.getDownloadByUrl(tabelle.getModel().getValueAt(delRow, DatenDownload.DOWNLOAD_URL_NR).toString());
                if (datenDownload.start != null) {
                    if (datenDownload.start.status <= Start.STATUS_RUN) {
                        wartenOderLaufen = true;
                    }
                }
            }
            // Download starten
            JMenuItem itemStarten = new JMenuItem("Download starten");
            itemStarten.setIcon(GetIcon.getIcon("download_start_16.png"));
            itemStarten.setEnabled(!wartenOderLaufen);
            jPopupMenu.add(itemStarten);
            itemStarten.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent arg0) {
                    filmStartenWiederholenStoppen(false /* alle */, true /* starten */);
                }
            });

            // Download stoppen
            JMenuItem itemStoppen = new JMenuItem("Download stoppen");
            itemStoppen.setIcon(GetIcon.getIcon("download_stop_16.png"));
            itemStoppen.setEnabled(wartenOderLaufen);
            jPopupMenu.add(itemStoppen);
            itemStoppen.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent arg0) {
                    filmStartenWiederholenStoppen(false /* alle */, false /* starten */);
                }
            });

            // Zielordner öffnen
            JMenuItem itemOeffnen = new JMenuItem("Zielordner öffnen");
            itemOeffnen.setIcon(GetIcon.getIcon("fileopen_16.png"));
            jPopupMenu.add(itemOeffnen);
            itemOeffnen.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent arg0) {
                    zielordnerOeffnen();
                }
            });


            //#######################################
            jPopupMenu.addSeparator();
            //#######################################

            JMenuItem itemVorziehen = new JMenuItem("Download vorziehen");
            itemVorziehen.setIcon(GetIcon.getIcon("move_up_16.png"));
            jPopupMenu.add(itemVorziehen);
            itemVorziehen.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent arg0) {
                    downloadsVorziehen();
                }
            });
            JMenuItem itemLoeschen = new JMenuItem("Download zurückstellen");
            itemLoeschen.setIcon(GetIcon.getIcon("undo_16.png"));
            jPopupMenu.add(itemLoeschen);
            itemLoeschen.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent arg0) {
                    downloadLoeschen(false /* dauerhaft */);
                }
            });
            //dauerhaft löschen
            JMenuItem itemDauerhaftLoeschen = new JMenuItem("Download dauerhaft löschen");
            itemDauerhaftLoeschen.setIcon(GetIcon.getIcon("download_del_16.png"));
            jPopupMenu.add(itemDauerhaftLoeschen);
            itemDauerhaftLoeschen.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent arg0) {
                    downloadLoeschen(true /* dauerhaft */);
                }
            });
            //ändern
            JMenuItem itemAendern = new JMenuItem("Download Ändern");
            itemAendern.setIcon(GetIcon.getIcon("configure_16.png"));
            jPopupMenu.add(itemAendern);
            itemAendern.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent arg0) {
                    downloadAendern();
                }
            });

            //#######################################
            jPopupMenu.addSeparator();
            //#######################################

            JMenuItem itemAlleStarten = new JMenuItem("alle Downloads starten");
            itemAlleStarten.setIcon(GetIcon.getIcon("download_alleStarten_16.png"));
            jPopupMenu.add(itemAlleStarten);
            itemAlleStarten.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent arg0) {
                    filmStartenWiederholenStoppen(true /* alle */, true /* starten */);
                }
            });
            JMenuItem itemAlleStoppen = new JMenuItem("alle Downloads stoppen");
            itemAlleStoppen.setIcon(GetIcon.getIcon("download_stop_16.png"));
            jPopupMenu.add(itemAlleStoppen);
            itemAlleStoppen.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent arg0) {
                    filmStartenWiederholenStoppen(true /* alle */, false /* starten */);
                }
            });
            JMenuItem itemWartendeStoppen = new JMenuItem("wartende Downloads stoppen");
            itemWartendeStoppen.setIcon(GetIcon.getIcon("download_stop_16.png"));
            jPopupMenu.add(itemWartendeStoppen);
            itemWartendeStoppen.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent arg0) {
                    wartendeDownloadsStoppen();
                }
            });
            JMenuItem itemAktualisieren = new JMenuItem("Liste der Downloads aktualisieren");
            itemAktualisieren.setIcon(GetIcon.getIcon("view-refresh_16.png"));
            jPopupMenu.add(itemAktualisieren);
            itemAktualisieren.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent arg0) {
                    downloadsAktualisieren();
                }
            });
            JMenuItem itemAufraeumen = new JMenuItem("Liste Aufräumen");
            itemAufraeumen.setIcon(GetIcon.getIcon("download_clear_16.png"));
            jPopupMenu.add(itemAufraeumen);
            itemAufraeumen.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent arg0) {
                    downloadsAufraeumen();
                }
            });

            //#######################################
            jPopupMenu.addSeparator();
            //#######################################
            // Film abspielen
            JMenuItem itemPlayerDownload = new JMenuItem("gespeicherten Film abspielen");
            itemPlayerDownload.setIcon(GetIcon.getIcon("film_play_16.png"));

            itemPlayerDownload.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    filmAbspielen_();
                }
            });
            jPopupMenu.add(itemPlayerDownload);

            // URL abspielen
            JMenuItem itemPlayer = new JMenuItem("Film (URL) abspielen");
            itemPlayer.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    int nr = tabelle.rowAtPoint(p);
                    if (nr >= 0) {
                        DatenPset gruppe = daten.listePset.getPsetAbspielen();
                        if (gruppe != null) {
                            int selectedModelRow = tabelle.convertRowIndexToModel(nr);
                            String url = tabelle.getModel().getValueAt(selectedModelRow, DatenDownload.DOWNLOAD_URL_NR).toString();
                            DatenDownload download = Daten.listeDownloads.getDownloadByUrl(url);
                            if (download != null) {
                                if (download.film == null) {
                                    // bei Einmaldownload nach Programmstart
                                    download.film = Daten.listeFilme.getFilmByUrl(url);
                                }
                                DatenFilm filmDownload = download.film.getCopy();
                                // und jetzt die tatsächlichen URLs des Downloads eintragen
                                filmDownload.arr[DatenFilm.FILM_URL_NR] = download.arr[DatenDownload.DOWNLOAD_URL_NR];
                                filmDownload.arr[DatenFilm.FILM_URL_RTMP_NR] = download.arr[DatenDownload.DOWNLOAD_URL_RTMP_NR];
                                filmDownload.arr[DatenFilm.FILM_URL_KLEIN_NR] = "";
                                filmDownload.arr[DatenFilm.FILM_URL_RTMP_KLEIN_NR] = "";
                                // und starten
                                daten.starterClass.urlMitProgrammStarten(gruppe, filmDownload);
                            }
                        } else {
                            String menuPath;
                            if (SystemInfo.isMacOSX()) {
                                menuPath = "MediathekView->Einstellungen…->Aufzeichnen und Abspielen";
                            } else {
                                menuPath = "Datei->Einstellungen->Aufzeichnen und Abspielen";
                            }
                            MVMessageDialog.showMessageDialog(parentComponent, "Bitte legen Sie im Menü \"" + menuPath + "\" ein Programm zum Abspielen fest.",
                                    "Kein Videoplayer!", JOptionPane.INFORMATION_MESSAGE);
                        }
                    }
                }
            });
            jPopupMenu.add(itemPlayer);

            // URL kopieren
            JMenuItem itemUrl = new JMenuItem("URL kopieren");
            itemUrl.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    int nr = tabelle.rowAtPoint(p);
                    if (nr >= 0) {
                        GuiFunktionen.copyToClipboard(
                                tabelle.getModel().getValueAt(tabelle.convertRowIndexToModel(nr),
                                DatenDownload.DOWNLOAD_URL_NR).toString());
                    }
                }
            });
            jPopupMenu.add(itemUrl);

            // Infos
            JMenuItem itemInfo = new JMenuItem("Infos anzeigen");
            itemInfo.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (!filmInfoHud.isVisible()) {
                        filmInfoHud.show();
                    }
                }
            });
            jPopupMenu.add(itemInfo);
            // ######################
            // Menü anzeigen
            jPopupMenu.show(evt.getComponent(), evt.getX(), evt.getY());
        }
    }

    private class BeobAnzeige implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            tabelleLaden();
        }
    }
}
