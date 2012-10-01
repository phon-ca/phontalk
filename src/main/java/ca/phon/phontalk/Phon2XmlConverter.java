package ca.phon.phontalk;

import ca.phon.application.IPhonFactory;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.CommonTreeNodeStream;

import ca.phon.application.transcript.ITranscript;
import ca.phon.application.transcript.IUtterance;
import ca.phon.application.transcript.TranscriptUtils;
import ca.phon.phontalk.parser.Phon2XmlWalker;
import ca.phon.system.logger.PhonLogger;

public class Phon2XmlConverter extends PhonConverter {
	
	public Phon2XmlConverter() {
		super();
	}
	
	/**
	 * Convert a transcript to an xml String.
	 * 
	 */
	@Override
	public String convertTranscript(ITranscript t) {
		// convert the transcript into a CHAT CommonTree
		PhonTreeBuilder builder = new PhonTreeBuilder();
		CommonTree sessionTree = builder.buildTree(t);
		
//		PhonTreeBuilder.printTree(sessionTree, 0);
		
		if(sessionTree == null) {
			PhonLogger.severe("Could not build CHAT tree.");
			return "";
		}
		CommonTreeNodeStream nodeStream = new CommonTreeNodeStream(sessionTree);
		Phon2XmlWalker walker = new Phon2XmlWalker(nodeStream);
		
		String retVal = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
		try {
			Phon2XmlWalker.chat_return v = walker.chat();
			retVal = v.st.toString();
		} catch (RecognitionException e) {
			PhonLogger.warning(e.toString());
		} catch (StackOverflowError err) {
			findStackOverflowError(t);
		}
		
		return retVal;
	}

	/**
	 * Method to try and locate the record which causes a
	 * StackOverflowError.  For this purpose we will be performing a
	 * conversion on a test session which contains only one record
	 * (iterating through the records in the given session.)
	 */
	private void findStackOverflowError(ITranscript t) {
		
		IPhonFactory factory = IPhonFactory.getFactory(t.getVersion());

		for(int i = 0; i < t.getNumberOfUtterances(); i++) {
			ITranscript testTranscript = factory.createTranscript();

			// copy session header and participant information
			TranscriptUtils.copyTranscriptInfo(t, testTranscript);

			IUtterance utt = t.getUtterances().get(i);
			TranscriptUtils.addRecordToTranscript(testTranscript, utt, null);

			// now attept to convert the test Transcript
			PhonTreeBuilder treeBuilder = new PhonTreeBuilder();
			CommonTree tTree = treeBuilder.buildTree(testTranscript);

			CommonTreeNodeStream nodeStream = new CommonTreeNodeStream(tTree);
			Phon2XmlWalker walker = new Phon2XmlWalker(nodeStream);

			try {
				Phon2XmlWalker.chat_return v = walker.chat();
				v.st.toString();
			} catch (RecognitionException e) {
				PhonLogger.warning(e.toString());
			} catch (StackOverflowError err) {
				PhonLogger.severe("Error processing record " + (i+1));
				break;
			}
		}

	}

}
