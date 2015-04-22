/*
 * Phon - An open source tool for research in phonology.
 * Copyright (C) 2005 - 2015, Gregory Hedlund <ghedlund@mun.ca> and Yvan Rose <yrose@mun.ca>
 * Dept of Linguistics, Memorial University <https://phon.ca>
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
package ca.phon.phontalk.plugin.wizard.talkbank2phon;

import java.awt.BorderLayout;
import java.io.File;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import ca.phon.application.project.IPhonProject;
import ca.phon.application.project.PhonProject;
import ca.phon.gui.DialogHeader;
import ca.phon.gui.components.FileSelectionField;
import ca.phon.gui.components.FileSelectionField.SelectionMode;
import ca.phon.gui.wizard.WizardStep;
import ca.phon.system.prefs.UserPrefManager;
import ca.phon.util.PhonUtilities;

public class NewProjectStep extends WizardStep {

	private static final long serialVersionUID = 3028232508966443872L;
	
	private FileSelectionField projectFolderField;
	
	private JTextField projectNameField;
	
	private JRadioButton workspaceButton;
	
	private JRadioButton otherLocationButton;

	public NewProjectStep() {
		super();
		
		init();
	}
	
	private void init() {
		this.projectFolderField = new FileSelectionField();
		projectFolderField.setMode(SelectionMode.FOLDERS);
		projectFolderField.setEnabled(false);
		projectFolderField.setFile(new File("."));
		
		projectNameField = new JTextField();
		
		final ButtonGroup btnGrp = new ButtonGroup();
		workspaceButton = new JRadioButton("Workspace");
		workspaceButton.setSelected(true);
		otherLocationButton = new JRadioButton("Other location:");
		
		btnGrp.add(workspaceButton);
		btnGrp.add(otherLocationButton);
		
		final FormLayout layout = new FormLayout(
				"5dlu, pref, 5dlu, pref, 5dlu, fill:pref:grow, 5dlu",
				"pref, pref, 3dlu, pref, pref, pref, pref");
		final JPanel centerPanel = new JPanel(layout);
		final CellConstraints cc = new CellConstraints();
		
		centerPanel.add(new JLabel("<html><b>Project Name:</b></html>"), cc.xy(2, 1));
		centerPanel.add(projectNameField, cc.xyw(4, 2, 3));
		
		centerPanel.add(new JLabel("<html><b>Project Location:</b></html>"), cc.xy(2, 4));
		centerPanel.add(workspaceButton, cc.xyw(4, 5, 3));
		centerPanel.add(otherLocationButton, cc.xy(2, 6));
		centerPanel.add(projectFolderField, cc.xy(6, 7));
		
		final DialogHeader header = new DialogHeader("TalkBank to Phon", "Enter name and location for new Phon project.");
		
		setLayout(new BorderLayout());
		add(header, BorderLayout.NORTH);
		add(centerPanel, BorderLayout.CENTER);
	}
	
	@Override
	public boolean validateStep() {
		// make sure a name is entered
		String projectName = projectNameField.getText();
		
		if(projectName.trim().length() == 0) {
			PhonUtilities.showComponentMessage(projectNameField, "Please enter a project name.");
			return false;
		} else {
			return true;
		}
		
	}
	
	public String getProjectLocation() {
		String retVal = null;
		
		if(workspaceButton.isSelected()) {
			retVal = PhonUtilities.getPhonWorkspace().getAbsolutePath();
		} else {
			retVal = projectFolderField.getSelectedFile().getAbsolutePath();
		}
		
		return retVal;
	}
	
	public IPhonProject getProject() throws IOException {
		final String loc = getProjectLocation();
		IPhonProject retVal = PhonProject.newProject(loc);
		return retVal;
	}
	
}
