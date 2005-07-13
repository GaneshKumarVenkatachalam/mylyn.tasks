/*******************************************************************************
 * Copyright (c) 2004 - 2005 University Of British Columbia and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     University Of British Columbia - initial API and implementation
 *******************************************************************************/

package org.eclipse.mylar.tasks;

import java.util.List;

import org.eclipse.jface.action.IAction;
import org.eclipse.mylar.tasks.ui.views.TaskListView;

/**
 * @author Mik Kersten
 * 
 * TODO: this should use extension points
 */
public interface ITaskListActionContributor {
	
	public abstract List<IAction> getToolbarActions(TaskListView view);

	public abstract List<IAction> getPopupActions(TaskListView view);
}
