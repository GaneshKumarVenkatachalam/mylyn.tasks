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
/*
 * Created on 31-Jan-2005
 */
package org.eclipse.mylar.tasks.ui;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.mylar.bugzilla.core.BugReport;
import org.eclipse.mylar.bugzilla.ui.editor.AbstractBugEditor;
import org.eclipse.mylar.bugzilla.ui.editor.ExistingBugEditor;
import org.eclipse.mylar.core.MylarPlugin;
import org.eclipse.mylar.tasks.BugzillaTask;
import org.eclipse.mylar.tasks.MylarTasksPlugin;
import org.eclipse.mylar.ui.MylarImages;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.internal.Workbench;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

/**
 * @author Eric Booth
 */
public class BugzillaTaskEditor extends MultiPageEditorPart {

	/** The task that created this editor */
	protected BugzillaTask bugTask;
	
	/** This bug report can be modified by the user and saved offline. */
	protected BugReport offlineBug;
	
	private ExistingBugEditor buzillaEditor;

	private BugzillaTaskEditorInput bugzillaEditorInput;
	
    private TaskSummaryEditor taskSummaryEditor = new TaskSummaryEditor();
    
	protected IContentOutlinePage outlinePage = null;
	
	public BugzillaTaskEditor() {
		super();

		// get the workbench page and add a listener so we can detect when it closes
		IWorkbench wb = MylarTasksPlugin.getDefault().getWorkbench();
		IWorkbenchWindow aw = wb.getActiveWorkbenchWindow();
		IWorkbenchPage ap = aw.getActivePage();
		BugzillaTaskEditorListener listener = new BugzillaTaskEditorListener();
		ap.addPartListener(listener);
		
		buzillaEditor = new ExistingBugEditor();
        taskSummaryEditor = new TaskSummaryEditor();
	}

    public AbstractBugEditor getBugzillaEditor(){
        return buzillaEditor;
    }
    
    public TaskSummaryEditor getTaskEditor(){
        return taskSummaryEditor;
    }
    
    
	public void gotoMarker(IMarker marker) {
		// don't do anything
	}
	
	/**
	 * Creates page 1 of the multi-page editor,
	 * which allows you to change the font used in page 2.
	 */
	private void createBugzillaSubmitPage() {
		buzillaEditor.createPartControl(getContainer());
		Composite composite = buzillaEditor.getEditorComposite();
		int index = addPage(composite);
		setPageText(index, "Bugzilla");
	}
    
    
    private void createSummaryPage() {
        try{
            int index = addPage(taskSummaryEditor, new TaskEditorInput(bugTask));
            setPageText(index, "Summary");         
        }catch(Exception e){
        	MylarPlugin.log(this.getClass().toString(), e);
        }
    }
	
	/**
	 * Creates the pages of the multi-page editor.
	 */
    @Override
	protected void createPages() {	
		createBugzillaSubmitPage();
        createSummaryPage();
	}
	
	/**
	 * Saves the multi-page editor's document.
	 */
    @Override
	public void doSave(IProgressMonitor monitor) {
		getEditor(0).doSave(monitor);
	}
	
	/**
	 * Saves the multi-page editor's document as another file.
	 * Also updates the text for page 0's tab, and updates this multi-page editor's input
	 * to correspond to the nested editor's.
	 */
    @Override
	public void doSaveAs() {
		IEditorPart editor = getEditor(0);
		editor.doSaveAs();
		setPageText(0, editor.getTitle());
		setInput(editor.getEditorInput());
	}
	
	@Override
	public void init(IEditorSite site, IEditorInput editorInput) throws PartInitException {
		if (!(editorInput instanceof BugzillaTaskEditorInput))
			throw new PartInitException("Invalid Input: Must be BugzillaTaskEditorInput");
		bugzillaEditorInput = (BugzillaTaskEditorInput) editorInput;
		bugTask = bugzillaEditorInput.getBugTask();

		offlineBug = bugzillaEditorInput.getOfflineBug();
		super.init(site, editorInput);
		super.setSite(site);
		super.setInput(editorInput);
		
		try {
			buzillaEditor.init(this.getEditorSite(), this.getEditorInput());
		}
		catch (Exception e) {
			throw new PartInitException(e.getMessage());
		}
		
		// Set the title on the editor's tab
		this.setPartName("Bug #" + bugzillaEditorInput.getBugId());
		this.setTitleImage(MylarImages.getImage(MylarImages.TASK_BUGZILLA));
	}
	
	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}
	
	/**
	 * Calculates the contents of page 2 when the it is activated.
	 */
	@Override
	protected void pageChange(int newPageIndex) {
		super.pageChange(newPageIndex);
	}
	
	/**
	 * Sets the font related data to be applied to the text in page 2.
	 */
	@Override
	public void setFocus() {
		// The default focus for this editor is the submit page
		buzillaEditor.setFocus();
	}

	/**
	 * @return Returns the bugTask.
	 */
	public BugzillaTask getBugTask() {
		return bugTask;
	}
	
	/**
	 * @return Returns the offlineBug.
	 */
	public BugReport getOfflineBug() {
		return offlineBug;
	}
	
	/**
	 * Updates the title of the editor to reflect dirty status.
	 * If the bug report has been modified but not saved, then
	 * an indicator will appear in the title.
	 * @param isDirty
	 *            is true when the bug report has been modified but not saved
	 */
	public void showDirtyStatus(boolean isDirty) {
		String prefix = (isDirty) ? "*" : "" ;
		setPartName(prefix + "Bug #" + bugzillaEditorInput.getBugId());
	}
	
	/**
	 * Class to listen for editor events
	 */
	private class BugzillaTaskEditorListener implements IPartListener
	{

		public void partActivated(IWorkbenchPart part) {
			// don't care about this event
		}

		public void partBroughtToTop(IWorkbenchPart part) {
			// don't care about this event
		}

		public void partClosed(IWorkbenchPart part) {

			// if we are closing a bug editor
			if (part instanceof BugzillaTaskEditor) {
				BugzillaTaskEditor taskEditor = (BugzillaTaskEditor)part;
				
				// check if it needs to be saved
				if (taskEditor.buzillaEditor.isDirty) {
					// ask the user whether they want to save it or not and perform the appropriate action
					taskEditor.buzillaEditor.changeDirtyStatus(false);
					boolean response = MessageDialog.openQuestion(null, "Save Changes", 
							"You have made some changes to the bug, do you want to save them?");
					if (response) {
						taskEditor.buzillaEditor.saveBug();
					}
				}
			}
		}

		public void partDeactivated(IWorkbenchPart part) {
			// don't care about this event
		}

		public void partOpened(IWorkbenchPart part) {
			// don't care about this event
		}
	}

	public void makeNewPage(BugReport serverBug, String newCommentText) {
		if (serverBug == null) {
			MessageDialog.openInformation(Workbench.getInstance().getActiveWorkbenchWindow().getShell(),
					"Could not open bug.", "Bug #" + offlineBug.getId()
									+ " could not be read from the server.  Try refreshing the bug task.");
			return;
		}
	}

	@Override
	public Object getAdapter(Class adapter) {
		return buzillaEditor.getAdapter(adapter);
	}
    
    public void close() {
        Display display= getSite().getShell().getDisplay();
        display.asyncExec(new Runnable() {
            public void run() {
                getSite().getPage().closeEditor(BugzillaTaskEditor.this, false);
            }
        });
    }
}
