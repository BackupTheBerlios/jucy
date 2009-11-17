package uc.protocols.client;

import java.io.IOException;
import java.net.ProtocolException;

import logger.LoggerFactory;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.eclipse.core.runtime.Platform;

import uc.protocols.ADCStatusMessage;

public class STA extends AbstractADCClientProtocolCommand {

	private static final Logger logger = LoggerFactory.make();
	static {
		logger.setLevel(Platform.inDevelopmentMode()? Level.DEBUG:Level.INFO);
	}
	
	public STA(ClientProtocol client) {
		super(client);
		setPattern(prefix+" ([012])(\\d{2}) ("+ADCTEXT+")(?: (.*))?",true);
	}


	public void handle(String command) throws ProtocolException, IOException {
		int severity = Integer.valueOf(matcher.group(1));
		int errorCode = Integer.valueOf(matcher.group(2));
		String message = revReplaces(matcher.group(3));
		
		ADCStatusMessage sm  = new ADCStatusMessage(message,severity,errorCode); 
		switch(errorCode) {
		case 53:
			client.noSlotsAvailable(message);
			break;
		}
		
		if (severity == ADCStatusMessage.FATAL) {
			client.otherSentError(sm.getMessage() == null ? message: sm.getMessage());
		} else {
			logger.debug("message received: "+sm.toString());
		}
	}
	
	public static void sendSTA(ClientProtocol client,ADCStatusMessage sm) {
		client.sendRaw("CSTA "+sm.toADCString()+"\n");
		if (sm.getSeverity() == ADCStatusMessage.FATAL) {
			client.disconnect(sm);
		}
	}
	
	

}