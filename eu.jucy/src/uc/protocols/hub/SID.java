package uc.protocols.hub;

import java.io.IOException;
import java.net.ProtocolException;

public class SID extends AbstractADCHubCommand {

	
	
	public SID(Hub hub) {
		super(hub);
		setPattern("ISID ("+SID+")",false);
	}


	public void handle(String command) throws ProtocolException, IOException {
		int s = SIDToInt(matcher.group(1));
		hub.getSelf().setSid( s ); 
		hub.insertUser(hub.getSelf());
		hub.removeCommand(this);
		hub.sendMyInfo(true);
	}

}
