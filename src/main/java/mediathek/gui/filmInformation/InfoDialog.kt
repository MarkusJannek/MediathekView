package mediathek.gui.filmInformation

import com.formdev.flatlaf.util.ScaledImageIcon
import com.jidesoft.swing.MultilineLabel
import mediathek.daten.Country
import mediathek.daten.DatenFilm
import mediathek.gui.actions.UrlHyperlinkAction
import mediathek.mainwindow.MediathekGui
import mediathek.tool.ApplicationConfiguration
import mediathek.tool.CopyToClipboardAction
import mediathek.tool.GuiFunktionen
import mediathek.tool.SwingErrorDialog
import mediathek.tool.sender_icon_cache.MVSenderIconCache
import net.miginfocom.layout.AC
import net.miginfocom.layout.CC
import net.miginfocom.layout.LC
import net.miginfocom.swing.MigLayout
import org.apache.commons.configuration2.sync.LockMode
import org.apache.commons.lang3.SystemUtils
import org.jdesktop.swingx.JXHyperlink
import java.awt.Desktop
import java.awt.Dimension
import java.awt.Point
import java.awt.Window
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.net.URI
import java.net.URISyntaxException
import java.util.stream.Collectors
import javax.swing.*


class InfoDialog(parent: Window?) : JDialog(parent) {
    private val config = ApplicationConfiguration.getConfiguration()
    private var currentFilm: DatenFilm? = null
    private val lblSender = JLabel()
    private val lblThema = MultilineLabel()
    private val lblTitel = MultilineLabel()
    private val lblDate = JLabel()
    private val lblUhrzeit = JLabel()
    private val lblDuration = JLabel()
    private val lblSize = JLabel()
    private val lblGeo = JLabel()
    private val cbHq = SwingDisabledCheckBox()
    private val cbSubtitle = SwingDisabledCheckBox()
    private val lblAbo = JLabel()
    private val hyperlink = SwingHyperlink()
    private val descScrollPane = JScrollPane()
    private val lblDescription = JTextArea()


    internal class SwingDisabledCheckBox : JCheckBox() {
        init {
            isEnabled = false
        }
    }

    /**
     * Restore window position from config settings.
     */
    private fun restoreLocation() {
        config.lock(LockMode.READ)
        try {
            val newLocation = Point()
            newLocation.x = config.getInt(ApplicationConfiguration.FilmInfoDialog.X)
            newLocation.y = config.getInt(ApplicationConfiguration.FilmInfoDialog.Y)
            location = newLocation

            if (SystemUtils.IS_OS_LINUX) {
                val w: Int = config.getInt(ApplicationConfiguration.FilmInfoDialog.WIDTH)
                val h: Int = config.getInt(ApplicationConfiguration.FilmInfoDialog.HEIGHT)
                if (w > 50 && h > 50)
                    size = Dimension(w, h)
            }
        } catch (ignored: NoSuchElementException) {
        } finally {
            config.unlock(LockMode.READ)
        }
    }

    /**
     * Save window position to config.
     */
    private fun saveLocation() {
        //prevent strange OpenJDK bug on Linux where getLocationOnScreen will fail if not visible...
        if (!isVisible) return
        config.lock(LockMode.WRITE)
        try {
            val location = locationOnScreen
            config.setProperty(ApplicationConfiguration.FilmInfoDialog.X, location.x)
            config.setProperty(ApplicationConfiguration.FilmInfoDialog.Y, location.y)
            if (SystemUtils.IS_OS_LINUX) {
                config.setProperty(ApplicationConfiguration.FilmInfoDialog.WIDTH, width)
                config.setProperty(ApplicationConfiguration.FilmInfoDialog.HEIGHT, height)
            }
        } finally {
            config.unlock(LockMode.WRITE)
        }
    }

    fun showInfo() {
        updateTextFields()
        if (!isVisible) isVisible = true
    }

    private fun clearControls() {
        lblThema.text = ""
        lblSender.text = ""
        lblSender.icon = null
        lblTitel.text = ""
        lblDate.text = ""
        lblUhrzeit.text = ""
        lblDuration.text = ""
        lblSender.text = ""
        lblGeo.text = ""
        cbHq.isSelected = false
        cbSubtitle.isSelected = false
        lblAbo.text = ""
        hyperlink.toolTipText = ""
        hyperlink.isEnabled = false
        hyperlink.componentPopupMenu = null
        lblDescription.text = ""
        SwingUtilities.invokeLater {
            checkedPack()
        }
    }

    companion object {
        private val DEFAULT_SENDER_DIMENSION = Dimension(64, 64)
    }

    private fun updateTextFields() {
        if (currentFilm == null) {
            clearControls()
        } else {
            MVSenderIconCache[currentFilm!!.sender].ifPresent { icon: ImageIcon? ->
                lblSender.text = ""
                val imageDim = Dimension(icon!!.iconWidth, icon.iconHeight)
                val destDim = GuiFunktionen.calculateFittedDimension(imageDim, DEFAULT_SENDER_DIMENSION)
                lblSender.icon = ScaledImageIcon(icon, destDim.width, destDim.height)
            }
            lblThema.text = currentFilm!!.thema
            lblTitel.text = currentFilm!!.title
            lblDate.text = currentFilm!!.sendeDatum
            lblUhrzeit.text = currentFilm!!.sendeZeit
            lblDuration.text = currentFilm!!.filmLengthAsString
            lblSize.text = currentFilm!!.fileSize.toString()

            if (currentFilm!!.countrySet.isEmpty())
                lblGeo.text = ""
            else
                lblGeo.text = currentFilm!!.countrySet.stream().map(Country::toString).collect(Collectors.joining("-"))

            cbHq.isSelected = currentFilm!!.isHighQuality
            cbSubtitle.isSelected = currentFilm!!.hasSubtitle()
            lblAbo.text = currentFilm!!.abo?.name
            hyperlink.isEnabled = true
            hyperlink.toolTipText = currentFilm!!.websiteUrl
            hyperlink.isClicked = false
            val popupMenu = JPopupMenu()
            popupMenu.add(CopyToClipboardAction(currentFilm!!.websiteUrl))
            hyperlink.componentPopupMenu = popupMenu

            val desc = currentFilm!!.description.trim { it <= ' ' }
            lblDescription.text = desc
            SwingUtilities.invokeLater {
                descScrollPane.verticalScrollBar.value = 0
                checkedPack()
            }
        }
    }

    /**
     * This will set the display to the new film data.
     *
     * @param film the film data
     */
    fun updateCurrentFilm(film: DatenFilm?) {
        currentFilm = film
        if (isVisible) {
            updateTextFields()
        }
    }

    internal class SwingHyperlink : JXHyperlink() {
        private fun openUrl(url: String) {
            try {
                UrlHyperlinkAction.openURL(null, url)
            } catch (ex: URISyntaxException) {
                ex.printStackTrace()
            }
        }

        init {
            this.text = "Link zur Webseite"

            addActionListener {
                if (toolTipText.isNotEmpty()) {
                    if (Desktop.isDesktopSupported()) {
                        val d = Desktop.getDesktop()
                        if (d.isSupported(Desktop.Action.BROWSE)) {
                            try {
                                d.browse(URI(toolTipText))
                            } catch (ex: Exception) {
                                SwingErrorDialog.showExceptionMessage(
                                    MediathekGui.ui(),
                                    "Es trat ein Fehler beim Öffnen des Links auf.\nSollte dies häufiger auftreten kontaktieren Sie bitte das Entwicklerteam.",
                                    ex
                                )
                            }
                        } else {
                            openUrl(toolTipText)
                        }
                    } else {
                        openUrl(toolTipText)
                    }

                }
            }
        }
    }

    private fun buildLayout() {
        layout = MigLayout(
            LC().insets("5").hideMode(3),
            // columns
            AC()
                .fill().gap()
                .size("250!").fill(),
            // rows
            AC()
                .gap()
                .gap()
                .gap()
                .gap()
                .gap()
                .gap()
                .gap()
                .gap()
                .gap()
                .gap()
                .gap()
                .gap()
                .gap()
                .gap()
        )

        add(JLabel("Sender:"), CC().cell(0, 0))
        add(lblSender, CC().cell(1, 0))
        add(JLabel("Thema:"), CC().cell(0, 1))
        lblThema.lineWrap = true
        add(lblThema, CC().cell(1, 1).growY())
        add(JLabel("Titel:"), CC().cell(0, 2))
        lblTitel.lineWrap = true
        add(lblTitel, CC().cell(1, 2).growY())

        add(JLabel("Datum:"), CC().cell(0, 3))
        add(lblDate, CC().cell(1, 3))
        add(JLabel("Uhrzeit:"), CC().cell(0, 4))
        add(lblUhrzeit, CC().cell(1, 4))
        add(JLabel("Dauer:"), CC().cell(0, 5))
        add(lblDuration, CC().cell(1, 5))
        add(JLabel("Größe (MB):"), CC().cell(0, 6))
        add(lblSize, CC().cell(1, 6))
        add(JLabel("HQ:"), CC().cell(0, 7))
        add(cbHq, CC().cell(1, 7))
        add(JLabel("Untertitel:"), CC().cell(0, 8))
        add(cbSubtitle, CC().cell(1, 8))
        add(JLabel("Geo:"), CC().cell(0, 9))
        add(lblGeo, CC().cell(1, 9))
        add(JLabel("Abo:"), CC().cell(0, 10))
        add(lblAbo, CC().cell(1, 10))
        add(hyperlink, CC().cell(0, 11).spanX(2))
        add(JLabel("Beschreibung:"), CC().cell(0, 12))
        lblDescription.isEditable = false
        lblDescription.lineWrap = true
        lblDescription.wrapStyleWord = true
        descScrollPane.setViewportView(lblDescription)
        add(descScrollPane, CC().cell(0, 13).spanX(2).growY().growX().height("90!"))
    }

    /**
     * Only pack dialog when we are NOT running linux...
     */
    private fun checkedPack() {
        //only pack only OS OTHER THAN Linux
        if (!SystemUtils.IS_OS_LINUX)
            pack()
    }

    init {
        type = Type.UTILITY
        title = "Filminformation"
        if (!SystemUtils.IS_OS_LINUX)
            isResizable = false
        defaultCloseOperation = DISPOSE_ON_CLOSE

        buildLayout()
        checkedPack()
        if (SystemUtils.IS_OS_LINUX) {
            val linuxSize = Dimension(350, 450)
            setSize(linuxSize.width, linuxSize.height)
            minimumSize = linuxSize
            preferredSize = linuxSize
            maximumSize = linuxSize
        }

        updateTextFields()
        restoreLocation()
        isVisible = config.getBoolean(ApplicationConfiguration.FilmInfoDialog.VISIBLE, false)

        addWindowListener(object : WindowAdapter() {
            override fun windowOpened(e: WindowEvent) {
                config.setProperty(ApplicationConfiguration.FilmInfoDialog.VISIBLE, true)
            }

            override fun windowClosed(e: WindowEvent) {
                config.setProperty(ApplicationConfiguration.FilmInfoDialog.VISIBLE, false)
            }
        })

        //addFilmlistLoadListener();
        addComponentListener(object : ComponentAdapter() {
            override fun componentResized(e: ComponentEvent?) {
                saveLocation()
            }

            override fun componentMoved(e: ComponentEvent) {
                if (isVisible) {
                    saveLocation()
                }
            }
        })
    }
}