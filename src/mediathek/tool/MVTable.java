/*    
 *    MediathekView
 *    Copyright (C) 2008   W. Xaver
 *    W.Xaver[at]googlemail.com
 *    http://zdfmediathk.sourceforge.net/
 *    
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package mediathek.tool;

import mediathek.controller.Log;
import java.awt.Cursor;
import java.awt.Rectangle;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DragSource;
import java.util.LinkedList;
import java.util.List;
import javax.activation.DataHandler;
import javax.swing.DropMode;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.RowSorter;
import javax.swing.RowSorter.SortKey;
import javax.swing.SortOrder;
import javax.swing.TransferHandler;
import mediathek.daten.Daten;
import mediathek.daten.DatenAbo;
import mediathek.daten.DatenDownload;
import mediathek.daten.DatenProg;
import mediathek.daten.DatenPset;
import msearch.daten.DatenFilm;

public final class MVTable extends JTable {

    public static final String TABELLEN = "Tabellen";
    public static final int TABELLE_STANDARD = -1;
    public static final int TABELLE_TAB_FILME = 0;
    public static final int TABELLE_TAB_DOWNLOADS = 1;
    public static final int TABELLE_TAB_ABOS = 2;
    public static final int TABELLE_TAB_PSET = 3;
    public static final int TABELLE_TAB_PROG = 4;
    public static final String FELDTRENNER = "|";
    public static final String SORT_ASCENDING = "ASCENDING";
    public static final String SORT_DESCENDING = "DESCENDING";
    private List<? extends RowSorter.SortKey> listeSortKeys = null;
    int[] breite;
    int[] reihe;
    private int indexSpalte = 0;
    private int selRow = -1;
    private int[] selRows;
    private String[] indexWertSelection = null;
    private int[] selIndexes = null;
    private boolean[] spaltenAnzeigen;
    //
    int nrDatenSystem = 0;
    int tabelle;
    String[] spaltenTitel;
    int maxSpalten;

    public MVTable(int ttabelle) {
        tabelle = ttabelle;
        this.setAutoCreateRowSorter(true);
        this.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_OFF);
        this.setRowHeight(18);
        switch (tabelle) {
            case TABELLE_TAB_FILME:
                spaltenTitel = DatenFilm.COLUMN_NAMES;
                maxSpalten = DatenFilm.MAX_ELEM;
                spaltenAnzeigen = getSpaltenEinAus(DatenFilm.spaltenAnzeigen, DatenFilm.MAX_ELEM);
                indexSpalte = DatenFilm.FILM_NR_NR;
                nrDatenSystem = Konstanten.SYSTEM_EIGENSCHAFTEN_TABELLE_FILME_NR;
                this.setModel(new TModelFilm(new Object[][]{}, spaltenTitel));
                break;
            case TABELLE_TAB_DOWNLOADS:
                spaltenTitel = DatenDownload.COLUMN_NAMES;
                maxSpalten = DatenDownload.MAX_ELEM;
                spaltenAnzeigen = getSpaltenEinAus(DatenDownload.spaltenAnzeigen, DatenDownload.MAX_ELEM);
                indexSpalte = DatenDownload.DOWNLOAD_NR_NR;
                //indexSpalte = DatenDownload.DOWNLOAD_URL_NR;
                nrDatenSystem = Konstanten.SYSTEM_EIGENSCHAFTEN_TABELLE_DOWNLOADS_NR;
                this.setDragEnabled(true);
                this.setDropMode(DropMode.INSERT_ROWS);
                this.setTransferHandler(new TableRowTransferHandlerDownload(this));
                this.setModel(new TModelDownload(new Object[][]{}, spaltenTitel));
                break;
            case TABELLE_TAB_ABOS:
                spaltenTitel = DatenAbo.COLUMN_NAMES;
                maxSpalten = DatenAbo.MAX_ELEM;
                spaltenAnzeigen = getSpaltenEinAus(DatenAbo.spaltenAnzeigen, DatenAbo.MAX_ELEM);
                indexSpalte = DatenAbo.ABO_NR_NR;
                nrDatenSystem = Konstanten.SYSTEM_EIGENSCHAFTEN_TABELLE_ABOS_NR;
                this.setModel(new TModelAbo(new Object[][]{}, spaltenTitel));
                break;
            case TABELLE_TAB_PSET:
                spaltenTitel = DatenPset.COLUMN_NAMES;
                maxSpalten = DatenPset.MAX_ELEM;
                spaltenAnzeigen = getSpaltenEinAus(DatenPset.spaltenAnzeigen, DatenPset.MAX_ELEM);
                indexSpalte = 0;
                nrDatenSystem = -1;
                this.setModel(new TModel(new Object[][]{}, spaltenTitel));
                this.setRowSorter(null);
                this.setAutoCreateRowSorter(false); // Reihenfolge ist die Anzeige der Button!
                break;
            case TABELLE_TAB_PROG:
                spaltenTitel = DatenProg.COLUMN_NAMES;
                maxSpalten = DatenProg.MAX_ELEM;
                spaltenAnzeigen = getSpaltenEinAus(DatenProg.spaltenAnzeigen, DatenProg.MAX_ELEM);
                indexSpalte = 0;
                nrDatenSystem = -1;
                this.setModel(new TModel(new Object[][]{}, spaltenTitel));
                break;
        }
        breite = getArray(maxSpalten);
        reihe = getArray(maxSpalten);
    }

    class TableRowTransferHandlerDownload extends TransferHandler {

        //private final DataFlavor localObjectFlavor = new ActivationDataFlavor(Integer.class, DataFlavor.javaJVMLocalObjectMimeType, "Integer Row Index");
        private final DataFlavor localObjectFlavor = new DataFlavor(Integer.class, "Integer Row Index");
        private JTable table = null;
        private int[] transferedRows = null;

        public TableRowTransferHandlerDownload(JTable table) {
            this.table = table;
        }

        @Override
        protected Transferable createTransferable(JComponent c) {
            assert (c == table);
            transferedRows = table.getSelectedRows();
            return new DataHandler(new Integer(table.getSelectedRow()), localObjectFlavor.getMimeType());
        }

        @Override
        public boolean canImport(TransferHandler.TransferSupport info) {
            try {
                boolean b = info.getComponent() == table && info.isDrop() && info.isDataFlavorSupported(localObjectFlavor);
                table.setCursor(b ? DragSource.DefaultMoveDrop : DragSource.DefaultMoveNoDrop);
                return b;
                // here's the problem
                // canImport is called during drags AND before drop is accepted
            } catch (Exception e) {
                e.printStackTrace();
            }
            return true;
        }

        @Override
        public int getSourceActions(JComponent c) {
            return TransferHandler.COPY_OR_MOVE;
        }

        @Override
        public boolean importData(TransferHandler.TransferSupport info) {
            try {
                JTable target = (JTable) info.getComponent();
                JTable.DropLocation dl = (JTable.DropLocation) info.getDropLocation();
                int index = dl.getRow();
                TModel tModel = (TModel) table.getModel();
                int max = tModel.getRowCount();
                if (index < 0 || index > max) {
                    index = max;
                }
                target.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                if (transferedRows != null) {
                    reorder(index, transferedRows);
                    transferedRows = null;
                    return true;
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return false;
        }

        @Override
        protected void exportDone(JComponent c, Transferable t, int act) {
            if (act == TransferHandler.MOVE) {
                table.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            }
        }
    }

    public void reorder(int index, int[] rowFrom) {
        getSelected();
        TModel tModel = (TModelDownload) this.getModel();
        // listeDownloads neu nach der Reihenfolge in der Tabelle erstellen
        for (int i = 0; i < this.getRowCount(); ++i) {
            DatenDownload d = ((DatenDownload) tModel.getValueAt(this.convertRowIndexToModel(i), DatenDownload.DOWNLOAD_REF_NR));
            if (d != null) {
                Daten.listeDownloads.remove(d);
                Daten.listeDownloads.add(d);
            }
        }
        // Downloads zum Verschieben suchen
        LinkedList<DatenDownload> liste = new LinkedList<>();
        for (int i = 0; i < rowFrom.length; ++i) {
            if (index > rowFrom[i]) {
                --index;
            }
            DatenDownload d = ((DatenDownload) tModel.getValueAt(this.convertRowIndexToModel(rowFrom[i]), DatenDownload.DOWNLOAD_REF_NR));
            liste.add(d);
            Daten.listeDownloads.remove(d);
        }
        // an der richtigen Stellei einfügen
        Daten.listeDownloads.addAll(index, liste);
        // die Tabellensortierung löschen, die wird jetzt mit der Liste wieder gefüllt
        this.getRowSorter().setSortKeys(null);
        this.setRowSorter(null);
        this.setAutoCreateRowSorter(true);
        setSelected();
        ListenerMediathekView.notify(ListenerMediathekView.EREIGNIS_REIHENFOLGE_DOWNLOAD, MVTable.class.getSimpleName());
    }

    public void initTabelle() {
        // Tabelle das erste Mal initialisieren,
        // mit den gespeicherten Daten oder
        // den Standardwerten
        // erst die Breite, dann die Reihenfolge
        try {
            if (nrDatenSystem == -1) {
                // wird nur für eingerichtete Tabellen gemacht
                return;
            }
            String b = "", r = "", s = "", upDown = "";
            boolean ok = false;
            if (!Daten.system[nrDatenSystem].equals("")) {
                ok = true;
                int f1, f2, f3;
                //String d = Daten.system[nrDatenSystem];
                if ((f1 = Daten.system[nrDatenSystem].indexOf(FELDTRENNER)) != -1) {
                    b = Daten.system[nrDatenSystem].substring(0, f1);
                    if ((f2 = Daten.system[nrDatenSystem].indexOf(FELDTRENNER, f1 + 1)) != -1) {
                        r = Daten.system[nrDatenSystem].substring(f1 + 1, f2);
                    }
                    if ((f3 = Daten.system[nrDatenSystem].indexOf(FELDTRENNER, f2 + 1)) != -1) {
                        s = Daten.system[nrDatenSystem].substring(f2 + 1, f3);
                        upDown = Daten.system[nrDatenSystem].substring(f3 + 1);
                    }
                }
                if (!arrLesen(b, breite)) {
                    ok = false;
                }
                if (!arrLesen(r, reihe)) {
                    ok = false;
                }
                SortKey sk = sortKeyLesen(s, upDown);
                if (sk != null) {
                    LinkedList<SortKey> listSortKeys_ = new LinkedList<>();
                    listSortKeys_.add(sk);
                    this.getRowSorter().setSortKeys(listSortKeys_);
                }
            }
            if (ok) {
                setSpaltenEinAus(breite, spaltenAnzeigen);
                setSpalten();
            } else {
                resetTabelle();
            }
        } catch (Exception ex) {
            //vorsichtshalber
        }
    }

    private boolean anzeigen(int i, boolean[] spaltenAnzeigen) {
        return spaltenAnzeigen[i];
    }

    private void setSpaltenEinAus(int[] nr, boolean[] spaltenAnzeigen) {
        for (int i = 0; i < spaltenAnzeigen.length; ++i) {
            spaltenAnzeigen[i] = nr[i] > 0;
        }
    }

    private boolean[] getSpaltenEinAus(boolean[] spaltenAnzeigen, int MAX_ELEM) {
//        spaltenAnzeigen = new boolean[MAX_ELEM];
        for (int i = 0; i < MAX_ELEM; ++i) {
            spaltenAnzeigen[i] = true;
        }
        return spaltenAnzeigen;
    }

    public void fireTableDataChanged(boolean setSpalten) {
//        this.selectionModel.setValueIsAdjusting(true);
        if (setSpalten) {
            getSelected();
        }
        ((TModel) this.getModel()).fireTableDataChanged();
        if (setSpalten) {
            setSelected();
        }
//        this.selectionModel.setValueIsAdjusting(false);
    }

//    public void clearSelected() {
//        clearSelection();
//        modelRowCount = -1;
//        modelSelections = null;
//    }
//    public void getSelected() {
//        // Einstellungen der Tabelle merken
//        if (this.getSelectedRow() >= 0) {
//            selection = this.getSelectedRows();
//            modelSelections = new int[selection.length];
//            for (int i = 0; i < selection.length; ++i) {
//                modelSelections[i] = this.convertRowIndexToModel(selection[i]);
//            }
//            modelRowCount = this.getRowCount();
//        } else {
//            modelSelections = null;
//            modelRowCount = -1;
//        }
//    }
//    public void setSelected() {
//        // gemerkte Einstellungen der Tabelle wieder setzten
//        stopBeob = true;
//        this.getSelectionModel().setValueIsAdjusting(true);
//        if (modelSelections != null && modelRowCount >= 0 && modelRowCount == this.getRowCount()) {
//            int b;
//            b = this.convertRowIndexToView(modelSelections[0]);
//            this.setRowSelectionInterval(b, b);
//            for (int i = 1; i < modelSelections.length; ++i) {
//                b = this.convertRowIndexToView(modelSelections[i]);
//                this.addRowSelectionInterval(b, b);
//            }
//        } else {
//            this.clearSelection();
//            if (this.getRowCount() > 0) {
//                this.setRowSelectionInterval(0, 0);
//            }
//        }
//        this.getSelectionModel().setValueIsAdjusting(false);
//        stopBeob = false;
//    }
    public void requestFocusSelelct(JScrollPane jScrollPane) {
        requestFocus();
        if (getRowCount() > 0) {
            // sonst ist schon eine Zeile markiert
            if (getSelectedRow() == -1) {
                setRowSelectionInterval(0, 0);
            }
            int firstSelectedRow = getSelectedRow();
            Rectangle cellLocation = getCellRect(firstSelectedRow, 0, false);
            jScrollPane.getVerticalScrollBar().setValue(cellLocation.y);
        }
    }

    public void requestFocusSelelct(JScrollPane jScrollPane, int zeile) {
        requestFocus();
        if (getRowCount() > 0) {
            // sonst ist schon eine Zeile markiert
            setRowSelectionInterval(zeile, zeile);
            int firstSelectedRow = getSelectedRow();
            Rectangle cellLocation = getCellRect(firstSelectedRow, 0, false);
            jScrollPane.getVerticalScrollBar().setValue(cellLocation.y);
        }
    }

    public void getSelected() {
        // Einstellungen der Tabelle merken
        selRow = this.getSelectedRow();
        selRows = this.getSelectedRows();
        switch (tabelle) {
            case TABELLE_TAB_DOWNLOADS:
            case TABELLE_TAB_FILME:
            case TABELLE_TAB_ABOS:
                if (selRow >= 0) {
                    selIndexes = new int[selRows.length];
                    int k = 0;
                    for (int i : selRows) {
                        selIndexes[k++] = (Integer) this.getModel().getValueAt(this.convertRowIndexToModel(i), indexSpalte);
                    }
                } else {
                    selIndexes = null;
                }
                break;
            default:
                if (selRows != null) {
                    if (selRows.length > 0) {
                        indexWertSelection = new String[selRows.length];
                        for (int i = 0; i < selRows.length; ++i) {
                            indexWertSelection[i] = this.getModel().getValueAt(this.convertRowIndexToModel(selRows[i]), indexSpalte).toString();
                        }
                    }
                }
                break;
        }
    }

    public void setSelected() {
        // gemerkte Einstellungen der Tabelle wieder setzten
        switch (tabelle) {
            case TABELLE_TAB_DOWNLOADS:
            case TABELLE_TAB_FILME:
            case TABELLE_TAB_ABOS:
                if (selIndexes != null) {
                    this.selectionModel.setValueIsAdjusting(true);
                    TModel tModel = (TModel) this.getModel();
                    int r;
                    for (int i : selIndexes) {
                        r = tModel.getIdxRow(i);
                        if (r >= 0) {
                            // ansonsten gibts die Zeile nicht mehr
                            r = this.convertRowIndexToView(r);
                            this.addRowSelectionInterval(r, r);
                        }
                    }
                    this.selectionModel.setValueIsAdjusting(false);
                }
                indexWertSelection = null;
                break;
            default:
                if (selRows != null) {
                    if (selRows.length > 0) {
                        this.selectionModel.setValueIsAdjusting(true);
                        for (int i = 0; i < selRows.length; ++i) {
                            if (selRows[i] < this.getRowCount()) {
                                this.addRowSelectionInterval(selRows[i], selRows[i]);
                            }
                        }
                        this.selectionModel.setValueIsAdjusting(false);
                    }
                }
                break;
        }
    }

    public void spaltenEinAus() {
        for (int i = 0; i < breite.length && i < this.getColumnCount(); ++i) {
            if (!anzeigen(i, spaltenAnzeigen)) {
                // geänderte Ansicht der Spalten abfragen
                breite[i] = 0;
            } else {
                if (breite[i] == 0) {
                    breite[i] = 100; // damit sie auch zu sehen ist :)
                }
            }
        }
        for (int i = 0; i < breite.length && i < this.getColumnCount(); ++i) {
            if (breite[i] == 0) {
                this.getColumnModel().getColumn(this.convertColumnIndexToView(i)).setMinWidth(0);
                this.getColumnModel().getColumn(this.convertColumnIndexToView(i)).setPreferredWidth(0);
                this.getColumnModel().getColumn(this.convertColumnIndexToView(i)).setMaxWidth(0);
            } else {
                this.getColumnModel().getColumn(this.convertColumnIndexToView(i)).setMinWidth(10);
                this.getColumnModel().getColumn(this.convertColumnIndexToView(i)).setMaxWidth(3000);
                this.getColumnModel().getColumn(this.convertColumnIndexToView(i)).setPreferredWidth(breite[i]);
            }
        }
        this.validate();
    }

    public void getSpalten() {
        // Einstellungen der Tabelle merken
        getSelected();
        for (int i = 0; i < reihe.length && i < this.getModel().getColumnCount(); ++i) {
            reihe[i] = this.convertColumnIndexToModel(i);
        }
        for (int i = 0; i < breite.length && i < this.getModel().getColumnCount(); ++i) {
            breite[i] = getColumnModel().getColumn(
                    this.convertColumnIndexToView(i)).getWidth();
        }
        if (this.getRowSorter() != null) {
            listeSortKeys = this.getRowSorter().getSortKeys();
        } else {
            listeSortKeys = null;
        }
    }

    public void setSpalten() {
        // gemerkte Einstellungen der Tabelle wieder setzten
        spaltenAusschalten();
        setSelected();
        try {
            for (int i = 0; i < breite.length && i < this.getColumnCount(); ++i) {
                if (!anzeigen(i, spaltenAnzeigen)) {
                    // geänderte Ansicht der Spalten abfragen
                    breite[i] = 0;
                } else {
                    if (breite[i] == 0) {
                        breite[i] = 100; // damit sie auch zu sehen ist :)
                    }
                }
            }
            for (int i = 0; i < breite.length && i < this.getColumnCount(); ++i) {
                if (breite[i] == 0) {
                    this.getColumnModel().getColumn(this.convertColumnIndexToView(i)).setMinWidth(0);
                    this.getColumnModel().getColumn(this.convertColumnIndexToView(i)).setPreferredWidth(0);
                    this.getColumnModel().getColumn(this.convertColumnIndexToView(i)).setMaxWidth(0);
                } else {
                    this.getColumnModel().getColumn(this.convertColumnIndexToView(i)).setMinWidth(10);
                    this.getColumnModel().getColumn(this.convertColumnIndexToView(i)).setMaxWidth(3000);
                    this.getColumnModel().getColumn(this.convertColumnIndexToView(i)).setPreferredWidth(breite[i]);
                }
            }
            for (int i = 0; i < reihe.length && i < this.getColumnCount(); ++i) {
                this.getColumnModel().moveColumn(this.convertColumnIndexToView(reihe[i]), i);
            }
            if (listeSortKeys != null) {
                if (!listeSortKeys.isEmpty()) {
                    this.getRowSorter().setSortKeys(listeSortKeys);
                }
            }
            this.validate();
        } catch (Exception ex) {
            Log.fehlerMeldung(965001463, Log.FEHLER_ART_PROG, "JTableMed.setSpalten", ex);
        }
    }

    public void resetTabelle() {
        // Standardwerte wetzen
        for (int i = 0; i < maxSpalten; ++i) {
            switch (tabelle) {
                case TABELLE_TAB_FILME:
                    reihe[i] = i;
                    breite[i] = 200;
                    if (i == DatenFilm.FILM_NR_NR) {
                        breite[i] = 75;
                    } else if (i == DatenFilm.FILM_TITEL_NR) {
                        breite[i] = 300;
                    } else if (i == DatenFilm.FILM_DATUM_NR
                            || i == DatenFilm.FILM_ZEIT_NR
                            || i == DatenFilm.FILM_SENDER_NR
                            || i == DatenFilm.FILM_GROESSE_NR
                            || i == DatenFilm.FILM_DAUER_NR) {
                        breite[i] = 100;
                    } else if (i == DatenFilm.FILM_URL_NR) {
                        breite[i] = 500;
                    } else if (i == DatenFilm.FILM_ABSPIELEN_NR
                            || i == DatenFilm.FILM_AUFZEICHNEN_NR) {
                        breite[i] = 50;
                    }
                    break;
                case TABELLE_TAB_DOWNLOADS:
                    reihe[i] = i;
                    breite[i] = 200;
                    if (i == DatenDownload.DOWNLOAD_NR_NR
                            || i == DatenDownload.DOWNLOAD_FILM_NR_NR) {
                        breite[i] = 75;
                    } else if (i == DatenDownload.DOWNLOAD_BUTTON_START_NR
                            || i == DatenDownload.DOWNLOAD_BUTTON_DEL_NR
                            || i == DatenDownload.DOWNLOAD_PROGRAMM_RESTART_NR) {
                        breite[i] = 50;
                    } else if (i == DatenDownload.DOWNLOAD_TITEL_NR) {
                        breite[i] = 250;
                    } else if (i == DatenDownload.DOWNLOAD_ABO_NR
                            || i == DatenDownload.DOWNLOAD_THEMA_NR) {
                        breite[i] = 150;
                    } else if (i == DatenDownload.DOWNLOAD_DATUM_NR
                            || i == DatenDownload.DOWNLOAD_ZEIT_NR
                            || i == DatenDownload.DOWNLOAD_GROESSE_NR
                            || i == DatenDownload.DOWNLOAD_BANDBREITE_NR
                            || i == DatenDownload.DOWNLOAD_SENDER_NR
                            || i == DatenDownload.DOWNLOAD_PROGRESS_NR
                            || i == DatenDownload.DOWNLOAD_RESTZEIT_NR
                            || i == DatenDownload.DOWNLOAD_DAUER_NR) {
                        breite[i] = 100;
                    }
                    break;
                case TABELLE_TAB_ABOS:
                    reihe[i] = i;
                    breite[i] = 200;
                    if (i == DatenAbo.ABO_NR_NR
                            || i == DatenAbo.ABO_EINGESCHALTET_NR) {
                        breite[i] = 75;
                    } else if (i == DatenAbo.ABO_DOWN_DATUM_NR
                            || i == DatenAbo.ABO_SENDER_NR) {
                        breite[i] = 100;
                    }
                    break;
                default:
                    break;
            }
        }
        listeSortKeys = null;
        this.getRowSorter().setSortKeys(null);
        this.setRowSorter(null);
        this.setAutoCreateRowSorter(true);
        spaltenAusschalten();
        setSpaltenEinAus(breite, spaltenAnzeigen);
        setSpalten();
    }

    private void spaltenAusschalten() {
        for (int i = 0; i < maxSpalten; ++i) {
            switch (tabelle) {
                case TABELLE_TAB_FILME:
                    if (i == DatenFilm.FILM_BESCHREIBUNG_NR
                            //                            || i == DatenFilm.FILM_KEYWORDS_NR
                            || i == DatenFilm.FILM_WEBSEITE_NR
                            || i == DatenFilm.FILM_IMAGE_URL_NR
                            || i == DatenFilm.FILM_URL_RTMP_NR
                            || i == DatenFilm.FILM_URL_AUTH_NR
                            || i == DatenFilm.FILM_URL_HD_NR
                            || i == DatenFilm.FILM_URL_RTMP_HD_NR
                            || i == DatenFilm.FILM_URL_KLEIN_NR
                            || i == DatenFilm.FILM_URL_RTMP_KLEIN_NR
                            || i == DatenFilm.FILM_DATUM_LONG_NR
                            || i == DatenFilm.FILM_REF_NR) {
                        breite[i] = 0;
                    }
                    break;
                case TABELLE_TAB_DOWNLOADS:
                    if (i == DatenDownload.DOWNLOAD_FILM_URL_NR
                            || i == DatenDownload.DOWNLOAD_URL_RTMP_NR
                            || i == DatenDownload.DOWNLOAD_URL_AUTH_NR
                            || i == DatenDownload.DOWNLOAD_PROGRAMM_NR
                            || i == DatenDownload.DOWNLOAD_PROGRAMM_AUFRUF_NR
                            || i == DatenDownload.DOWNLOAD_PROGRAMM_RESTART_NR
                            || i == DatenDownload.DOWNLOAD_ZIEL_DATEINAME_NR
                            || i == DatenDownload.DOWNLOAD_ZIEL_PFAD_NR
                            || i == DatenDownload.DOWNLOAD_ART_NR
                            || i == DatenDownload.DOWNLOAD_QUELLE_NR
                            || i == DatenDownload.DOWNLOAD_ZURUECKGESTELLT_NR
                            || i == DatenDownload.DOWNLOAD_REF_NR) {
                        breite[i] = 0;
                    }
                    break;
                case TABELLE_TAB_ABOS:
                    break;
                case TABELLE_STANDARD:
                    break;
            }
        }
    }

    public void tabelleNachDatenSchreiben() {
        if (tabelle == TABELLE_STANDARD) {
            // wird nur für eingerichtet Tabellen gemacht
            return;
        }
        // Tabellendaten ind die Daten.system schreiben
        // erst die Breite, dann die Reihenfolge
        String b, r, s = "", upDown = "";
        int reihe_[] = new int[maxSpalten];
        int breite_[] = new int[maxSpalten];
        for (int i = 0; i < reihe_.length && i < this.getModel().getColumnCount(); ++i) {
            reihe_[i] = this.convertColumnIndexToModel(i);
        }
        for (int i = 0; i < breite_.length && i < this.getModel().getColumnCount(); ++i) {
            breite_[i] = this.getColumnModel().getColumn(this.convertColumnIndexToView(i)).getWidth();
        }
        b = Integer.toString(breite_[0]);
        r = Integer.toString(reihe_[0]);
        for (int i = 1; i < breite.length; i++) {
            b = b + "," + Integer.toString(breite_[i]);
            r = r + "," + Integer.toString(reihe_[i]);
        }
        listeSortKeys = this.getRowSorter().getSortKeys();
        if (listeSortKeys != null) {
            if (!listeSortKeys.isEmpty()) {
                SortKey sk = listeSortKeys.get(0);
                s = String.valueOf(sk.getColumn());
                upDown = sk.getSortOrder().equals(SortOrder.ASCENDING) ? SORT_ASCENDING : SORT_DESCENDING;
            }
        }
        Daten.system[nrDatenSystem] = b + FELDTRENNER + r + FELDTRENNER + s + FELDTRENNER + upDown;
    }

    private int[] getArray(int anzahl) {
        int[] arr = new int[anzahl];
        for (int i = 0; i < arr.length; ++i) {
            arr[i] = -1;
        }
        return arr;
    }

    private boolean arrLesen(String s, int[] arr) {
        String sub;
        if (maxSpalten != countString(s)) {
            // dann hat sich die Anzahl der Spalten der Tabelle geändert: Versionswechsel
            return false;
        } else {
            for (int i = 0; i < maxSpalten; i++) {
                if (!s.equals("")) {
                    if (s.contains(",")) {
                        sub = s.substring(0, s.indexOf(","));
                        s = s.replaceFirst(sub + ",", "");
                    } else {
                        sub = s;
                        s = "";
                    }
                    try {
                        arr[i] = Integer.parseInt(sub);
                    } catch (Exception ex) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private SortKey sortKeyLesen(String s, String upDown) {
        SortKey sk;
        int sp;
        try {
            sp = Integer.parseInt(s);
            if (upDown.equals(SORT_DESCENDING)) {
                sk = new SortKey(sp, SortOrder.DESCENDING);
            } else {
                sk = new SortKey(sp, SortOrder.ASCENDING);
            }
        } catch (Exception ex) {
            return null;
        }
        return sk;
    }

    private int countString(String s) {
        int ret = 0;
        for (int i = 0; i < s.length(); ++i) {
            if (s.charAt(i) == ',') {
                ++ret;
            }
        }
        return ++ret;
    }
}
