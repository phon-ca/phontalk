package ca.phon.phontalk.plugin;

import java.io.*;
import java.util.concurrent.atomic.AtomicReference;

import ca.phon.app.log.LogUtil;
import ca.phon.phontalk.*;
import ca.phon.plugin.IPluginExtensionFactory;
import ca.phon.plugin.IPluginExtensionPoint;
import ca.phon.session.Session;
import ca.phon.session.io.*;
import ca.phon.ui.nativedialogs.OSInfo;

@SessionIO(
		group="org.talkbank",
		id="CHAT",
		version="0.0.1",
		mimetype="application/text",
		extension="cha",
		name="CHAT"
)
public class CHATSessionReader implements SessionReader, IPluginExtensionPoint<SessionReader> {

	public CHATSessionReader() {
	}

	@Override
	public boolean canRead(File file) throws IOException {
		final boolean extOk = file.getName().endsWith(".cha");
		
		// TODO check for @UTF8 header
		if(extOk) {
			try(InputStreamReader in = new InputStreamReader(new FileInputStream(file))) {
				final char[] buffer = new char[5];
				in.read(buffer, 0, buffer.length);
				
				final String testStr = new String(buffer);
				return testStr.equals("@UTF8");
			} catch (IOException e) {
				LogUtil.severe(e);
				return false;
			}
		} else
			return false;
	}

	@Override
	public Session readSession(InputStream stream) throws IOException {
		final File tempChaFile = File.createTempFile("chatter", "cha");
		tempChaFile.deleteOnExit();
		final File tempFile = File.createTempFile("chatter", "toxml");
		tempFile.deleteOnExit();
		
		try(final PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(tempChaFile), "UTF-8"))) {
			final BufferedReader in = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
			String line = null;
			while((line = in.readLine()) != null) {
				writer.write(line);
				writer.write(OSInfo.isMacOs() ? "\n" : "\r\n");
			}
			writer.flush();
			in.close();
		} catch (IOException e) {
			throw e;
		}
		
		final StringBuffer buffer = new StringBuffer();
		final AtomicReference<Boolean> xmlReady = new AtomicReference<Boolean>(true);
		// use the converter to convert to XML
		final CHAT2XmlConverter converter = new CHAT2XmlConverter();
		converter.convertFile(tempChaFile, tempFile, new PhonTalkListener() {
			
			@Override
			public void message(PhonTalkMessage msg) {
				xmlReady.set(false);
				buffer.append(msg.getMessage());
				buffer.append("\n");
			}
			
		});
		
		if(xmlReady.get()) {
			Xml2PhonConverter xml2Phon = new Xml2PhonConverter();
			xml2Phon.setInputFile(tempFile);
			final Session retVal =  xml2Phon.convertStream(new FileInputStream(tempFile), new PhonTalkListener() {
				
				@Override
				public void message(PhonTalkMessage msg) {
					buffer.append(msg.getMessage());
				}
			});
			retVal.putExtension(OriginalFormat.class, new OriginalFormat(getClass().getAnnotation(SessionIO.class)));
			return retVal;
		} else {
			throw new IOException(buffer.toString());
		}
		
	}

	@Override
	public Class<?> getExtensionType() {
		return SessionReader.class;
	}

	@Override
	public IPluginExtensionFactory<SessionReader> getFactory() {
		return (args) -> { return new CHATSessionReader(); };
	}
	
}
