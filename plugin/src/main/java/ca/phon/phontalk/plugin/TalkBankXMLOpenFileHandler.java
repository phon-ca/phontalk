package ca.phon.phontalk.plugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.xml.stream.events.StartElement;

import ca.phon.app.session.SessionFileOpenHandler;
import ca.phon.session.Session;

public class TalkBankXMLOpenFileHandler extends SessionFileOpenHandler {

	@Override
	public boolean canRead(StartElement startEle) {
		if(startEle.getName().getNamespaceURI().equals("http://www.talkbank.org/ns/talkbank") 
				&& startEle.getName().getLocalPart().equals("CHAT")) {
			return true;
		} else {
			return false;
		}
	}
	
}
