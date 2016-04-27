/*
    Copywrite 2012-2016 Will Winder

    This file is part of Universal Gcode Sender (UGS).

    UGS is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    UGS is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with UGS.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.willwinder.universalgcodesender.uielements.mainwindow;

import com.willwinder.universalgcodesender.GrblController;
import com.willwinder.universalgcodesender.i18n.Localization;
import com.willwinder.universalgcodesender.listeners.ControllerListener;
import com.willwinder.universalgcodesender.listeners.UGSEventListener;
import com.willwinder.universalgcodesender.model.BackendAPI;
import com.willwinder.universalgcodesender.model.GUIBackend;
import com.willwinder.universalgcodesender.model.Position;
import com.willwinder.universalgcodesender.model.UGSEvent;
import com.willwinder.universalgcodesender.types.GcodeCommand;
import com.willwinder.universalgcodesender.uielements.ConnectionSettingsDialog;
import com.willwinder.universalgcodesender.uielements.GrblFirmwareSettingsDialog;
import com.willwinder.universalgcodesender.utils.SettingsFactory;
import com.willwinder.universalgcodesender.utils.Version;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentEvent;

import static com.willwinder.universalgcodesender.utils.GUIHelpers.displayErrorDialog;

/**
 *
 * @author wwinder
 */
public class MainWindow extends JFrame implements ControllerListener {
    final private static String VERSION = Version.getVersion() + " / " + Version.getTimestamp();

    private com.willwinder.universalgcodesender.uielements.action.ActionPanel actionPanel;
    private com.willwinder.universalgcodesender.uielements.command.CommandPanel commandPanel;
    private com.willwinder.universalgcodesender.uielements.connection.ConnectionPanel connectionPanel;
    private JTabbedPane controlContextTabbedPane;
    private JMenu firmwareSettingsMenu;
    private JMenuItem grblConnectionSettingsMenuItem;
    private JMenuItem grblFirmwareSettingsMenuItem;
    private JScrollPane macroEditPanel;
    private com.willwinder.universalgcodesender.uielements.MacroPanel macroPanel;
    private JMenuBar mainMenuBar;
    private com.willwinder.universalgcodesender.uielements.pendant.PendantMenu pendantMenu;
    private JMenu settingsMenu;
    private com.willwinder.universalgcodesender.visualizer.VisualizerPanel visualizerPanel;


    BackendAPI backend;
    
    public MainWindow() throws Exception {
        this.backend = new GUIBackend();
        backend.applySettings(SettingsFactory.loadSettings());
        backend.addControllerListener(this);

        showNightlyWarning();
        initComponents();
        initProgram();
        loadSettings();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                saveSettings();
            }
        });
    }

    private void saveSettings() {
        commandPanel.saveSettings();
        connectionPanel.saveSettings();
        SettingsFactory.saveSettings(backend.getSettings());
    }

    private void loadSettings() {
        commandPanel.loadSettings();
        connectionPanel.loadSettings();
        visualizerPanel.loadSettings();
    }

    private void showNightlyWarning() {
        if (backend.getSettings().isShowNightlyWarning() && MainWindow.VERSION.contains("nightly")) {
            EventQueue.invokeLater(() -> {
                String message =
                        "This version of Universal Gcode Sender is a nightly build.\n"
                                + "It contains all of the latest features and improvements, \n"
                                + "but may also have bugs that still need to be fixed.\n"
                                + "\n"
                                + "If you encounter any problems, please report them on github.";
                JOptionPane.showMessageDialog(new JFrame(), message,
                        "", JOptionPane.INFORMATION_MESSAGE);
            });
        }
    }

    private void initComponents() {
        setLookAndFeel();
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(800, 600));
        setSize(backend.getSettings().getMainWindowSettings().width, backend.getSettings().getMainWindowSettings().height);
        setLocation(backend.getSettings().getMainWindowSettings().xLocation, backend.getSettings().getMainWindowSettings().yLocation);


        controlContextTabbedPane = new JTabbedPane();
        actionPanel = new com.willwinder.universalgcodesender.uielements.action.ActionPanel(backend);
        controlContextTabbedPane.addTab(Localization.getString("mainWindow.swing.controlContextTabbedPane.machineControl"), actionPanel);
        macroPanel = new com.willwinder.universalgcodesender.uielements.MacroPanel(backend);
        macroEditPanel = new JScrollPane();
        macroEditPanel.setViewportView(macroPanel);
        macroEditPanel.setToolTipText(Localization.getString("mainWindow.swing.macroInstructions"));
        controlContextTabbedPane.addTab(Localization.getString("mainWindow.swing.controlContextTabbedPane.macros"), macroEditPanel);
        visualizerPanel = new com.willwinder.universalgcodesender.visualizer.VisualizerPanel(backend);
        controlContextTabbedPane.addTab(Localization.getString("mainWindow.swing.visualizeButton"), visualizerPanel);

        connectionPanel = new com.willwinder.universalgcodesender.uielements.connection.ConnectionPanel(backend);
        commandPanel = new com.willwinder.universalgcodesender.uielements.command.CommandPanel(backend);

        pendantMenu = new com.willwinder.universalgcodesender.uielements.pendant.PendantMenu(backend);
        pendantMenu.setText(Localization.getString("PendantMenu.title"));

        grblConnectionSettingsMenuItem = new JMenuItem();
        grblConnectionSettingsMenuItem.setText(Localization.getString("mainWindow.swing.grblConnectionSettingsMenuItem"));
        grblConnectionSettingsMenuItem.addActionListener(evt -> grblConnectionSettingsMenuItemActionPerformed(evt));
        settingsMenu = new JMenu();
        settingsMenu.setText(Localization.getString("mainWindow.swing.settingsMenu"));
        settingsMenu.add(grblConnectionSettingsMenuItem);
        firmwareSettingsMenu = new JMenu(Localization.getString("mainWindow.swing.firmwareSettingsMenu"));
        grblFirmwareSettingsMenuItem = new JMenuItem(Localization.getString("mainWindow.swing.grblFirmwareSettingsMenuItem"));
        grblFirmwareSettingsMenuItem.addActionListener(evt -> grblFirmwareSettingsMenuItemActionPerformed(evt));
        firmwareSettingsMenu.add(grblFirmwareSettingsMenuItem);
        settingsMenu.add(firmwareSettingsMenu);
        mainMenuBar = new JMenuBar();
        mainMenuBar.add(settingsMenu);
        mainMenuBar.add(pendantMenu);
        setJMenuBar(mainMenuBar);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, controlContextTabbedPane, commandPanel);
        splitPane.setOneTouchExpandable(true);
//        splitPane.setDividerLocation(150);

        MigLayout layout = new MigLayout("fill", "[min!][]");
        setLayout(layout);

        add(connectionPanel, "grow");
        add(splitPane, "grow");
//        add(controlContextTabbedPane, "grow, wrap");
//        add(commandPanel, "grow");
    }

    private void setLookAndFeel() {
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(MainWindow.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(MainWindow.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(MainWindow.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(MainWindow.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
    }


    // TODO: It would be nice to streamline this somehow...
    private void grblConnectionSettingsMenuItemActionPerformed(java.awt.event.ActionEvent evt) {
        ConnectionSettingsDialog gcsd = new ConnectionSettingsDialog(backend.getSettings(), this, true);

        gcsd.setVisible(true);

        if (gcsd.saveChanges()) {
            try {
                backend.applySettings(backend.getSettings());
            } catch (Exception e) {
                displayErrorDialog(e.getMessage());
            }
        }
    }

    private void grblFirmwareSettingsMenuItemActionPerformed(java.awt.event.ActionEvent evt) {
        try {
            if (!this.backend.isConnected()) {
                displayErrorDialog(Localization.getString("mainWindow.error.noFirmware"));
            } else if (this.backend.getController() instanceof GrblController) {
                    GrblFirmwareSettingsDialog gfsd = new GrblFirmwareSettingsDialog(this, true, this.backend);
                    gfsd.setVisible(true);
            } else {
                displayErrorDialog(Localization.getString("mainWindow.error.notGrbl"));
            }
        } catch (Exception ex) {
                displayErrorDialog(ex.getMessage());
        }
    }

    private void initProgram() {
        Localization.initialize(this.backend.getSettings().getLanguage());
        try {
            backend.applySettings(backend.getSettings());
        } catch (Exception e) {
            displayErrorDialog(e.getMessage());
        }

        this.setTitle(Localization.getString("title") + " (" + Localization.getString("version") + " " + VERSION + ")");
    }

    @Override
    public void fileStreamComplete(String filename, boolean success) {
        final String durationLabelCopy = connectionPanel.getDuration();
        if (success) {
            EventQueue.invokeLater(() -> {
                JOptionPane.showMessageDialog(new JFrame(),
                        Localization.getString("mainWindow.ui.jobComplete") + " " + durationLabelCopy,
                        Localization.getString("success"), JOptionPane.INFORMATION_MESSAGE);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {}

            });
        } else {
            displayErrorDialog(Localization.getString("mainWindow.error.jobComplete"));
        }
    }

    @Override
    public void commandSkipped(GcodeCommand command) {

    }

    @Override
    public void commandSent(final GcodeCommand command) {

    }

    @Override
    public void commandComment(String comment) {

    }

    @Override
    public void commandComplete(final GcodeCommand command) {

    }

    @Override
    public void messageForConsole(MessageType type, String msg) {

    }

    @Override
    public void statusStringListener(String state, Position machineCoord, Position workCoord) {

    }

    @Override
    public void postProcessData(int numRows) {
    }
}
