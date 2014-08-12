package ca.phon.phontalk.app;

import java.io.File;

public interface PhonTalkDropListener {
	
	public void dropPhonSession(File file);
	
	public void dropPhonProject(File file);
	
	public void dropTalkBankFile(File file);
	
	public void dropTalkBankFolder(File file);

}
