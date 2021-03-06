/*******************************************************************************
 * Copyright (c) 2004, 2009 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/

package org.eclipse.mylyn.internal.tasks.ui.editors;

import org.eclipse.swt.graphics.Image;

/**
 * @author Shawn Minto
 * @deprecated use {@link org.eclipse.mylyn.commons.workbench.BusyAnimator.IBusyClient} instead
 */
@Deprecated
public interface IBusyEditor {

	public void setTitleImage(Image image);

	public Image getTitleImage();

}
