package ca.phon.phontalk.plugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

import org.apache.commons.io.FilenameUtils;

import ca.phon.app.actions.OpenFileHandler;
import ca.phon.app.log.LogUtil;
import ca.phon.app.modules.EntryPointArgs;
import ca.phon.app.project.DesktopProject;
import ca.phon.app.project.DesktopProjectFactory;
import ca.phon.app.session.editor.SessionEditor;
import ca.phon.app.session.editor.SessionEditorEP;
import ca.phon.plugin.IPluginExtensionFactory;
import ca.phon.plugin.IPluginExtensionPoint;
import ca.phon.plugin.PluginEntryPointRunner;
import ca.phon.project.Project;
import ca.phon.project.exceptions.ProjectConfigurationException;
import ca.phon.session.Session;
import ca.phon.session.io.SessionInputFactory;
import ca.phon.session.io.SessionReader;
import ca.phon.ui.CommonModuleFrame;

public class CHATOpenFileHandler implements OpenFileHandler, IPluginExtensionPoint<OpenFileHandler> {

	@Override
	public Set<String> supportedExtensions() {
		return Set.of("cha");
	}

	@Override
	public boolean canOpen(File file) throws IOException {
		CHATSessionReader reader = new CHATSessionReader();
		return reader.canRead(file);
	}

	@Override
	public void openFile(File file, Map<String, Object> args) throws IOException {
		SessionEditor existingEditor = findEditorForFile(file);
		if(existingEditor != null) {
			existingEditor.toFront();
			return;
		}
		
		Session session = openSession(file);
		if(session.getName() == null || session.getName().trim().length() == 0) {
			session.setName(FilenameUtils.removeExtension(file.getName()));
		}
		
		Project project = findProjectForFile(file);
		if(project == null) {
			project = createTempProjectForFile(file);
		}
		
		final EntryPointArgs epArgs = new EntryPointArgs(args);
		epArgs.put(EntryPointArgs.PROJECT_OBJECT, project);
		epArgs.put(EntryPointArgs.SESSION_OBJECT, session);
		PluginEntryPointRunner.executePluginInBackground(SessionEditorEP.EP_NAME, epArgs);
	}
	
	private SessionEditor findEditorForFile(File file) {
		for(CommonModuleFrame cmf:CommonModuleFrame.getOpenWindows()) {
			if(cmf instanceof SessionEditor) {
				SessionEditor editor = (SessionEditor)cmf;
				
				Project project = editor.getProject();
				Session session = editor.getSession();
				String sessionPath = project.getSessionPath(session);
				File sessionFile = new File(sessionPath);
				
				if(sessionFile.equals(file)) {
					return editor;
				}
			}
		}
		
		return null;
	}
	
	protected Session openSession(File file) throws IOException {
		SessionInputFactory factory = new SessionInputFactory();
		SessionReader reader = factory.createReaderForFile(file);
		Session session = reader.readSession(new FileInputStream(file));
		session.setCorpus(file.getParentFile().getName());
		return session;
	}
	
	protected Project createTempProjectForFile(File file) {
		File tmpFolder = new File(System.getProperty("java.io.tmpdir"));
		File projectFolder = new File(tmpFolder, UUID.randomUUID().toString());
		projectFolder.mkdirs();
		
		try {
			DesktopProject project = new DesktopProject(projectFolder);
			project.setName("Temp");
			project.addCorpus(file.getParentFile().getName(), "");
			project.setCorpusPath(file.getParentFile().getName(), file.getParentFile().getAbsolutePath());
			project.setCorpusMediaFolder(file.getParentFile().getName(), file.getParentFile().getAbsolutePath());
			
			return project;
		} catch (ProjectConfigurationException | IOException e) {
			LogUtil.warning(e);
		}
		return null;
	}
	
	protected Project findProjectForFile(File file) {
		File corpusFolder = file.getParentFile();
		File projectFolder = corpusFolder.getParentFile();
		
		// see if project is already open
		for(CommonModuleFrame cmf:CommonModuleFrame.getOpenWindows()) {
			Project windowProj = cmf.getExtension(Project.class);
			if(windowProj != null) {
				File windowProjFolder = new File(windowProj.getLocation());
				if(windowProjFolder.equals(projectFolder)) {
					return windowProj;
				}
			}
		}
		
		try {
			return (new DesktopProjectFactory()).openProject(projectFolder);
		} catch (IOException | ProjectConfigurationException e) {
			return null;
		}
	}

	@Override
	public Class<?> getExtensionType() {
		return OpenFileHandler.class;
	}

	@Override
	public IPluginExtensionFactory<OpenFileHandler> getFactory() {
		return (args) -> this;
	}

}
