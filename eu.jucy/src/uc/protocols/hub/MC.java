package uc.protocols.hub;

import java.io.IOException;
import java.util.regex.Pattern;

import uc.User;
import uc.protocols.DCProtocol;
import uc.protocols.hub.IFeedListener.FeedType;

/**
 * special protocol message for received main chat messages..
 * 
 * @author Quicksilver
 *
 */
public class MC extends AbstractNMDCHubProtocolCommand {

	private static final Pattern USER = Pattern.compile("<("+NICK+")> ("+TEXT+")");
	
	public MC(Hub hub) {
		super(hub);
		setPattern(TEXT,true);
	}

	@Override
	public void handle(String command) throws IOException {
		matcher = USER.matcher(command);
		if (matcher.matches()) {
			User usr = hub.getUserByNick( matcher.group(1) );
			if (usr != null) {
				String message = DCProtocol.reverseReplaces(matcher.group(2));
				if (usr.isOp() && message.contains("is kicking ") && message.contains(" because:")) {
					message = command;
					if (message.contains( "is kicking because:")) {
						message = command.replace( "is kicking because:", "").trim();
					}
					//is kicking because:  is sort of the a message targeting at feed..
					hub.feedReceived(FeedType.KICK,message);	
				} else {
					
					hub.mcMessageReceived(usr, message,false);
				}
			} else {
				hub.mcMessageReceived(null, DCProtocol.reverseReplaces(command),true);
			}
		} else {
			hub.mcMessageReceived(null, DCProtocol.reverseReplaces(command),true);
		}
	}

	@Override
	public String getPrefix() {
		return "MC";
	}
	
	public static void sendMainchatMessage(Hub hub,String message, boolean me) {
		String msg = DCProtocol.doReplaces(message);
		if (me) {
			hub.sendUnmodifiedRaw("*"+hub.getSelf().getNick()+" "+msg+"*|");
		} else {
			hub.sendUnmodifiedRaw("<"+hub.getSelf().getNick()+"> "+msg+"|");
		}
	}
	
	

}