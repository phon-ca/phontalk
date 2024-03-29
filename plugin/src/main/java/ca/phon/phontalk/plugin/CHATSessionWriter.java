package ca.phon.phontalk.plugin;

import java.awt.*;
import java.io.*;
import java.util.concurrent.atomic.AtomicReference;

import ca.phon.app.log.LogUtil;
import ca.phon.phontalk.*;
import ca.phon.plugin.IPluginExtensionFactory;
import ca.phon.plugin.IPluginExtensionPoint;
import ca.phon.session.Session;
import ca.phon.session.io.*;

@SessionIO(
		group="org.talkbank",
		id="CHAT",
		version="0.0.1",
		mimetype="application/text",
		extension="cha",
		name="CHAT (.cha)"
)
public class CHATSessionWriter implements SessionWriter, IPluginExtensionPoint<SessionWriter> {

	private PhonTalkListener listener;

	final PhonTalkListener defaultListener = (PhonTalkMessage msg) -> {
		LogUtil.warning(msg.toString());
	};

	public CHATSessionWriter() {
		this(null);
	}

	public CHATSessionWriter(PhonTalkListener listener) {
		this.listener = listener;
	}

	public PhonTalkListener getListener() {
		return (this.listener == null ? defaultListener : this.listener);
	}

	@Override
	public void writeSession(Session session, OutputStream out) throws IOException {
		final File tempXmlFile = File.createTempFile("phontalk", ".xml");
		tempXmlFile.deleteOnExit();
		// convert session to temporary xml file
		final TalkBankSessionWriter tbWriter = new TalkBankSessionWriter(getListener());
		tbWriter.writeSession(session, new FileOutputStream(tempXmlFile));
		
		// convert xml file to temp .cha file
		final File tempChaFile = File.createTempFile("phontalk", ".cha");
		tempChaFile.deleteOnExit();
		final StringBuffer buffer = new StringBuffer();
		final AtomicReference<Boolean> chaReady = new AtomicReference<Boolean>(true);
		// use the converter to convert to XML
		final Xml2CHATConverter converter = new Xml2CHATConverter();
		converter.convertFile(tempXmlFile, tempChaFile, new PhonTalkListener() {
			
			@Override
			public void message(PhonTalkMessage msg) {
				chaReady.set(false);
				buffer.append(msg.getMessage());
				buffer.append("\n");
			}
			
		});
		
		System.out.println(buffer.toString());
		
		// copy data to output stream
		if(chaReady.get()){
			final BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(tempChaFile), "UTF-8"));
			final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, "UTF-8"));
			String line = null;
			while((line = reader.readLine()) != null) {
				writer.write(line);
				writer.write("\n");
			}
			writer.flush();
			reader.close();
			writer.close();
		} else {
			throw new IOException(buffer.toString());
		}
	}

	@Override
	public Class<?> getExtensionType() {
		return SessionWriter.class;
	}

	@Override
	public IPluginExtensionFactory<SessionWriter> getFactory() {
		return (args) -> { return new CHATSessionWriter(null); };
	}
}
