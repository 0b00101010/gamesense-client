package com.gamesense.api.util.misc;

import com.gamesense.client.GameSenseMod;

import javax.swing.JLabel;
import javax.swing.JEditorPane;
import javax.swing.JOptionPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.Desktop;
import java.awt.Font;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Scanner;

/**
 * @author Hoosiers
 * @since 12/15/2020
 */

public class VersionChecker {

    public VersionChecker() {
        checkVersion(GameSenseMod.MODVER);
    }

    private void checkVersion(String version) {
        boolean isLatest = true;
        String newVersion = "null";

        if (version.startsWith("dev")) {
            return;
        }

        try {
            URL url = new URL("https://pastebin.com/raw/QxR9CS76");
            Scanner scanner = new Scanner(url.openStream());

            String grabbedVersion = scanner.next();

            if (!version.equalsIgnoreCase(grabbedVersion)) {
                isLatest = false;
                newVersion = grabbedVersion;
            }
        }
        catch (IOException e) {
            e.printStackTrace();
            isLatest = true;
        }

        if (!isLatest) {
            generatePopUp(newVersion);
        }
    }

    //thank god for stack overflow... https://stackoverflow.com/questions/8348063/clickable-links-in-joptionpane
    private void generatePopUp(String newVersion) {
        JLabel label = new JLabel();
        Font font = label.getFont();

        StringBuffer style = new StringBuffer("font-family:" + font.getFamily() + ";");
        style.append("font-weight:" + (font.isBold() ? "bold" : "normal") + ";");
        style.append("font-size:" + font.getSize() + "pt;");

        JEditorPane editorPane = new JEditorPane("text/html", "<html><body style=\"" + style + "\">" + "Version outdated! Download the latest (" + newVersion + ") " + "<a href=\"https://github.com/IUDevman/gamesense-client/releases\">HERE</a>" + "!" + "</body></html>");

        editorPane.addHyperlinkListener(new HyperlinkListener() {

            @Override
            public void hyperlinkUpdate(HyperlinkEvent event) {

                if (event.getEventType().equals(HyperlinkEvent.EventType.ACTIVATED)) {

                    try {
                        Desktop.getDesktop().browse(event.getURL().toURI());
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                    }
                    catch (URISyntaxException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        editorPane.setEditable(false);
        editorPane.setBackground(label.getBackground());

        // show
        JOptionPane.showMessageDialog(null, editorPane, GameSenseMod.MODNAME + " " + GameSenseMod.MODVER, JOptionPane.WARNING_MESSAGE);
    }
}