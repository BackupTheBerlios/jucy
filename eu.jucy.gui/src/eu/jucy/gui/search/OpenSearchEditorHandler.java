/**
 * 
 */
package eu.jucy.gui.search;

import java.util.Collections;

import logger.LoggerFactory;

import helpers.GH;

import org.apache.log4j.Logger;
import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.IHandlerService;

import uc.crypto.HashValue;

import eu.jucy.gui.OpenEditorHandler;

public class OpenSearchEditorHandler extends OpenEditorHandler {
	
	private static final Logger logger = LoggerFactory.make();
	
	public static final String COMMAND_ID = "eu.jucy.gui.OpenSearchEditor";
	public static final String INITIAL_SEARCH= "eu.jucy.gui.initialsearch";
	
	public OpenSearchEditorHandler() {
		super(SearchEditor.ID, new SearchEditorInput());
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		String search = event.getParameter(INITIAL_SEARCH);
		
		if (!GH.isNullOrEmpty(search)) {
			if (HashValue.isHash(search)) {
				input =  new SearchEditorInput(HashValue.createHash(search));
			} else {
				input =  new SearchEditorInput(search);
			}
		} else {
			input = new SearchEditorInput();
		}
		return super.execute(event);
	}
	
	/**
	 * 
	 * @param window  where to open it
	 * @param initialsearch - if initially there should be something on search
	 * may be null for nothing
	 */
	public static void openSearchEditor(IWorkbenchWindow window,String initialsearch) {
		IHandlerService handlerService = (IHandlerService) window.getService(IHandlerService.class);
		ICommandService comservice = (ICommandService)window.getService(ICommandService.class);
		
		try {
			
			Command com = comservice.getCommand(OpenSearchEditorHandler.COMMAND_ID);
			ParameterizedCommand p = ParameterizedCommand.generateCommand(
					com, Collections.singletonMap(OpenSearchEditorHandler.INITIAL_SEARCH, initialsearch));
			
			handlerService.executeCommand(p, null);
			
		} catch (Exception e1) {
			logger.warn(e1,e1);
		}
	}
	
}