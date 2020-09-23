package ca.phon.phontalk.plugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import ca.phon.app.log.LogUtil;
import ca.phon.phontalk.PhonTalkListener;
import ca.phon.phontalk.PhonTalkMessage;
import ca.phon.phontalk.PhonTalkTask;
import ca.phon.phontalk.PhonTalkMessage.Severity;

public class CopyFilePhonTalkTask extends PhonTalkTask {

	public CopyFilePhonTalkTask(File inputFile, File outputFile, PhonTalkListener listener) {
		super(inputFile, outputFile, listener);
	}

	@Override
	public String getProcessName() {
		return "copy";
	}

	@Override
	public void performTask() {
		setStatus(TaskStatus.RUNNING);
		
		Path inputPath = getInputFile().toPath();
		Path outputPath = getOutputFile().toPath();
		
		try {
			Files.copy(inputPath, outputPath, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			getListener().message(new PhonTalkMessage(e.getLocalizedMessage(), Severity.SEVERE));
			LogUtil.severe(e);
			super.err = e;
			setStatus(TaskStatus.ERROR);
		}
		
		if(getStatus() == TaskStatus.RUNNING)
			setStatus(TaskStatus.FINISHED);
	}

}
