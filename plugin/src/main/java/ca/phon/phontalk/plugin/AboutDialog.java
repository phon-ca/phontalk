/*
 * Phon - An open source tool for research in phonology.
 * Copyright (C) 2008 The Phon Project, Memorial University <http://phon.ling.mun.ca>
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package ca.phon.phontalk.plugin;

import ca.phon.application.PhonTask;
import ca.phon.gui.components.ActionLabel;
import ca.phon.system.logger.PhonLogger;
import ca.phon.util.OpenFileLauncher;
import com.jgoodies.forms.factories.ButtonBarFactory;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.event.MouseInputAdapter;

/**
 * Basic about dialog
 */
public class AboutDialog extends JDialog {

	private final static String WEBSITE_URL =
			"http://phon.ling.mun.ca/phontrac/wiki/phontalk";

	private final static String LICENCE_URL =
			"http://www.gnu.org/licenses/gpl-3.0.html";

	public AboutDialog() {
		super();
		super.setTitle("About PhonTalk");

		super.setModal(true);
		super.setResizable(false);

		init();
	}

	private void init() {
		setLayout(new BorderLayout());

		String lblTxt = "PhonTalk v 1.4";

		JLabel titleLbl = new JLabel(lblTxt);
		Font titleFont = titleLbl.getFont();
		titleFont = titleFont.deriveFont(Font.BOLD);
		titleFont = titleFont.deriveFont(14.0f);
		titleLbl.setFont(titleFont);
		add(titleLbl, BorderLayout.NORTH);

		FormLayout layout = new FormLayout(
				"right:100px, 5px, left:pref",
				"pref, 5px, pref, 5px, pref");
		CellConstraints cc = new CellConstraints();

		JPanel infoPanel = new JPanel();
		infoPanel.setLayout(layout);

		infoPanel.add(new JLabel("Build Date:"), cc.xy(1,1));
//		infoPanel.add(new JLabel(PhonTalk.PHONTALK_COMPILE_DATE), cc.xy(3,1));

		infoPanel.add(new JLabel("Website:"), cc.xy(1,3));
		JLabel websiteLbl = new JLabel("<html><u>http://phon.ling.mun.ca/</u></html>");
//		websiteLbl.set_action(new GotoWebsiteAction(WEBSITE_URL));
		websiteLbl.setToolTipText(WEBSITE_URL);
		websiteLbl.addMouseListener(new MouseInputAdapter() {

			@Override
			public void mousePressed(MouseEvent me) {
				try {
					OpenFileLauncher.launchBrowser(new URL(WEBSITE_URL));
				} catch (MalformedURLException ex) {
					PhonLogger.warning(ex.toString());
				}
			}

		});
		websiteLbl.setForeground(Color.blue);
		infoPanel.add(websiteLbl, cc.xy(3,3));

		infoPanel.add(new JLabel("Licence:"), cc.xy(1,5));
		JLabel licenceLbl = new JLabel("<html><u>GPLv3</u></html>");
		licenceLbl.addMouseListener(new MouseInputAdapter() {

			@Override
			public void mousePressed(MouseEvent me) {
				try {
					OpenFileLauncher.launchBrowser(new URL(LICENCE_URL));
				} catch (MalformedURLException ex) {
					PhonLogger.warning(ex.toString());
				}
			}

		});
//		licenceLbl.set_action(new GotoWebsiteAction(LICENCE_URL));
		licenceLbl.setToolTipText(LICENCE_URL);
		licenceLbl.setForeground(Color.blue);
		infoPanel.add(licenceLbl, cc.xy(3, 5));

		add(infoPanel, BorderLayout.CENTER);

		JButton closeBtn = new JButton("Close");
		closeBtn.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent ae) {
				AboutDialog.this.setVisible(false);
				AboutDialog.this.dispose();
			}
			
		});
		JComponent btnPanel =
				ButtonBarFactory.buildOKBar(closeBtn);
		add(btnPanel, BorderLayout.SOUTH);
	}

	private class GotoWebsiteAction extends PhonTask {
		private String url;

		public GotoWebsiteAction(String url) {
			this.url = url;
		}

		@Override
		public void performTask() {
			try {
				OpenFileLauncher.launchBrowser(new URL(url));
			} catch (MalformedURLException ex) {
				PhonLogger.warning(ex.toString());
			}
		}


	}
}
