/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package eu.jucy.gui.update;

import org.eclipse.equinox.internal.provisional.p2.ui.policy.IUViewQueryContext;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.Policy;

/**
 * CloudPolicy defines the RCP Cloud Example policies for the p2 UI. The policy
 * is registered as an OSGi service when the example bundle starts.
 * 
 * @since 3.5
 */
public class CloudPolicy extends Policy {
	public CloudPolicy() {
		// XXX User has no access to manipulate repositories
		setRepositoryManipulator(null);

		// XXX Default view is by category
		IUViewQueryContext queryContext = new IUViewQueryContext(
				IUViewQueryContext.AVAILABLE_VIEW_BY_CATEGORY);
		
		setQueryContext(queryContext);
		//TODO .. here changes to query context might be able to change what is seen..
	}
}
