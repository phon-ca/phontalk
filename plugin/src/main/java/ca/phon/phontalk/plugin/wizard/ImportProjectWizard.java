package ca.phon.phontalk.plugin.wizard;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;

import ca.phon.app.log.LogUtil;
import ca.phon.app.workspace.Workspace;
import ca.phon.phontalk.plugin.CHATSessionReader;
import ca.phon.phontalk.plugin.TalkBankSessionReader;
import ca.phon.ui.decorations.DialogHeader;
import ca.phon.ui.nativedialogs.FileFilter;
import ca.phon.ui.text.FileSelectionField;
import ca.phon.ui.text.PromptedTextField;
import ca.phon.ui.text.FileSelectionField.SelectionMode;
import ca.phon.ui.tristatecheckbox.TristateCheckBoxState;
import ca.phon.ui.tristatecheckbox.TristateCheckBoxTree;
import ca.phon.ui.tristatecheckbox.TristateCheckBoxTreeCellEditor;
import ca.phon.ui.tristatecheckbox.TristateCheckBoxTreeCellRenderer;
import ca.phon.ui.tristatecheckbox.TristateCheckBoxTreeModel;
import ca.phon.ui.tristatecheckbox.TristateCheckBoxTreeNode;
import ca.phon.ui.wizard.BreadcrumbWizardFrame;
import ca.phon.ui.wizard.WizardStep;

public class ImportProjectWizard extends BreadcrumbWizardFrame {

	private WizardStep folderStep;
	private FileSelectionField importFolderField;
	private FileSelectionField outputFolderField;
	private PromptedTextField projectNameField;
	private TristateCheckBoxTree fileSelectionTree;
	
	public ImportProjectWizard() {
		super("PhonTalk - Import Project");
		
		init();
	}
	
	private void init() {
		setupFolderStep();
		
		addWizardStep(folderStep);
	}
	
	private void setupFolderStep() {
		folderStep = new WizardStep();
		folderStep.setLayout(new BorderLayout());
		
		DialogHeader header = new DialogHeader("PhonTalk (xml2phon)", "Create a new Phon project from a folder of CHAT (.cha) or TalkBank (.xml) files");
		folderStep.add(header, BorderLayout.NORTH);
		
		importFolderField = new FileSelectionField();
		importFolderField.setMode(SelectionMode.FOLDERS);
		importFolderField.addPropertyChangeListener(FileSelectionField.FILE_PROP, e -> {
			if(importFolderField.getSelectedFile() != null) {
				TristateCheckBoxTreeNode treeNode = scanFolder(importFolderField.getSelectedFile());
				treeNode.setCheckingState(TristateCheckBoxState.CHECKED);
				TristateCheckBoxTreeModel treeModel = new TristateCheckBoxTreeModel(treeNode);
				fileSelectionTree.setRootVisible(true);
				fileSelectionTree.setModel(treeModel);
				
				setupProjectName();
			} else {
				fileSelectionTree.setModel(new TristateCheckBoxTreeModel(new TristateCheckBoxTreeNode()));
				fileSelectionTree.setRootVisible(false);
			}
		});
		
		outputFolderField = new FileSelectionField();
		outputFolderField.setMode(SelectionMode.FOLDERS);
		outputFolderField.setFile(Workspace.userWorkspaceFolder());
		
		projectNameField = new PromptedTextField("Project name");
		
		JPanel folderSelectionPanel = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.fill = GridBagConstraints.NONE;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		
		folderSelectionPanel.add(new JLabel("Import folder:"), gbc);
		++gbc.gridy;
		folderSelectionPanel.add(new JLabel("Output folder:"), gbc);
		++gbc.gridy;
		folderSelectionPanel.add(new JLabel("Project name:"), gbc);
		
		gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1.0;
		
		folderSelectionPanel.add(importFolderField, gbc);
		++gbc.gridy;
		folderSelectionPanel.add(outputFolderField, gbc);
		++gbc.gridy;
		folderSelectionPanel.add(projectNameField, gbc);
		
		fileSelectionTree = new TristateCheckBoxTree();
		fileSelectionTree.setRootVisible(false);
		fileSelectionTree.setCellRenderer(new CellRenderer());
		fileSelectionTree.setCellEditor(new TristateCheckBoxTreeCellEditor(fileSelectionTree, new CellRenderer()));
		
		JScrollPane scroller = new JScrollPane(fileSelectionTree);
		scroller.setBorder(BorderFactory.createTitledBorder("Select files for import"));
		
		JPanel centerPanel = new JPanel(new BorderLayout());
		centerPanel.add(folderSelectionPanel, BorderLayout.NORTH);
		centerPanel.add(scroller, BorderLayout.CENTER);
		
		folderStep.setTitle("Select files");
		folderStep.add(centerPanel, BorderLayout.CENTER);
	}
	
	private TristateCheckBoxTreeNode scanFolder(File folder) {
		TristateCheckBoxTreeNode retVal = new TristateCheckBoxTreeNode(folder);
		retVal.setEnablePartialCheck(false);
		
		FileFilter chatFilter = new FileFilter("CHAT files", "cha");
		
		for(File file:folder.listFiles()) {
			if(file.isDirectory()) {
				TristateCheckBoxTreeNode subtree = scanFolder(file);
				retVal.add(subtree);
			} else if(FileFilter.xmlFilter.accept(file)) {
				TalkBankSessionReader tbReader = new TalkBankSessionReader();
				try {
					if(tbReader.canRead(file)) {
						TristateCheckBoxTreeNode fileNode = new TristateCheckBoxTreeNode(file);
						retVal.add(fileNode);
					}
				} catch (IOException e) {
					LogUtil.severe(e);
				}
			} else if(chatFilter.accept(file)) {
				CHATSessionReader reader = new CHATSessionReader();
				try {
					if(reader.canRead(file)) {
						TristateCheckBoxTreeNode fileNode = new TristateCheckBoxTreeNode(file);
						fileNode.setEnablePartialCheck(false);
						retVal.add(fileNode);
					}
				} catch (IOException e) {
					LogUtil.severe(e);
				}
			}
		}
		
		return retVal;
	}
	
	private void setupProjectName() {
		File outputFolder = outputFolderField.getSelectedFile();
		if(outputFolder == null || !outputFolder.isDirectory()) return;
		
		File importFolder = importFolderField.getSelectedFile();
		if(importFolder == null || !importFolder.isDirectory()) return;
		
		int idx = 0;
		String projectName = importFolder.getName();
		File projectFile = new File(outputFolder, projectName);
		while(projectFile.exists()) {
			projectName = String.format("%s (%d)", importFolder.getName(), ++idx);
			projectFile = new File(outputFolder, projectName);
		}
		
		projectNameField.setText(projectName);
	}
	
	private class CellRenderer extends TristateCheckBoxTreeCellRenderer {

		@Override
		public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded,
				boolean leaf, int row, boolean hasFocus) {
			TristateCheckBoxTreeCellRenderer.TristateCheckBoxTreeNodePanel retVal = (TristateCheckBoxTreeCellRenderer.TristateCheckBoxTreeNodePanel)super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
			
			if(value instanceof TristateCheckBoxTreeNode
					&& value != tree.getModel().getRoot()) {
				TristateCheckBoxTreeNode node = (TristateCheckBoxTreeNode) value;
				TristateCheckBoxTreeNode parent = (TristateCheckBoxTreeNode)node.getParent();
				
				File parentFolder = (File)parent.getUserObject();
				File childFile = (File)node.getUserObject();
				
				Path folderPath = parentFolder.toPath();
				Path childPath = childFile.toPath();
				
				Path relativePath = folderPath.relativize(childPath);
				retVal.getLabel().setText(relativePath.toString());
			}
			
			return retVal;
		}
		
	}
		
	public static void main(String[] args) {
		ImportProjectWizard wizard = new ImportProjectWizard();
		wizard.pack();
		wizard.setSize(1024, 720);
		wizard.centerWindow();
		wizard.setVisible(true);
	}
	
}
