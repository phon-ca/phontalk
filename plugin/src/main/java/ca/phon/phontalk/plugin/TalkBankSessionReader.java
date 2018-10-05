package ca.phon.phontalk.plugin;

import java.io.*;

import javax.xml.namespace.QName;
import javax.xml.stream.*;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.XMLEvent;

import org.apache.commons.lang3.StringUtils;

import ca.phon.phontalk.*;
import ca.phon.plugin.IPluginExtensionFactory;
import ca.phon.plugin.IPluginExtensionPoint;
import ca.phon.session.Session;
import ca.phon.session.io.*;

@SessionIO(
		group="org.talkbank",
		id="talkbank",
		version="2.5.0",
		mimetype="application/xml",
		extension="xml",
		name="TalkBank"
)
public class TalkBankSessionReader implements SessionReader, IPluginExtensionPoint<SessionReader> {

	@Override
	public Session readSession(InputStream stream) throws IOException {
		final Xml2PhonConverter converter = new Xml2PhonConverter();
		converter.setInputFile(new File("unknown.xml"));
		final PhonTalkListener listener = (PhonTalkMessage msg) -> {
			// TODO add to list of session warnings
			System.out.println(msg);
		};
		
		Session retVal = converter.convertStream(stream, listener);
		// ID is no longer provided in TalkBank XML, Phon will fix this later
		retVal.setName("");
		retVal.putExtension(OriginalFormat.class, new OriginalFormat(getClass().getAnnotation(SessionIO.class)));
		return retVal;
	}

	@Override
	public boolean canRead(File file) throws IOException {
		// open file and make sure the first
		// element is 'session' with the correct version
		boolean canRead = false;
		
		// use StAX to read only first element
		// create StAX reader
		XMLInputFactory factory = XMLInputFactory.newInstance();
		XMLEventReader reader = null;
		try(FileInputStream source = new FileInputStream(file)) {
			//BufferedReader in = new BufferedReader(new InputStreamReader(source, "UTF-8"));
			XMLEventReader xmlReader = factory.createXMLEventReader(source, "UTF-8");
			reader = factory.createFilteredReader(xmlReader, new XMLWhitespaceFilter());

			XMLEvent evt;
			while(!(evt = reader.nextEvent()).isStartElement());
			Attribute versionAttr = evt.asStartElement().getAttributeByName(new QName("Version"));
			if(versionAttr == null) {
				canRead = false;
			} else {
				String version = versionAttr.getValue();
	
				canRead = 
						evt.asStartElement().getName().getLocalPart().equals("CHAT")
						&& (version.compareTo("2.1.0") >= 0);
			}
		} catch (XMLStreamException e) {
			throw new IOException(e);
		}
		
		return canRead;
	}
	
	private class XMLWhitespaceFilter implements EventFilter {

		@Override
		public boolean accept(XMLEvent arg0) {
			boolean retVal = true;
			
			
			if(arg0.isCharacters() && 
					StringUtils.strip(arg0.asCharacters().getData()).length() == 0) {
				
				retVal = false;
			}
			
			return retVal;
		}

	}

	@Override
	public Class<?> getExtensionType() {
		return SessionReader.class;
	}

	@Override
	public IPluginExtensionFactory<SessionReader> getFactory() {
		return (args) -> { return new TalkBankSessionReader(); };
	}

}
