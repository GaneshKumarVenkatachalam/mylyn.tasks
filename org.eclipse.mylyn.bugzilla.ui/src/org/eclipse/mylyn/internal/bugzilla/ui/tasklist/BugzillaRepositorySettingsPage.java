/*******************************************************************************
 * Copyright (c) 2004 - 2006 University Of British Columbia and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     University Of British Columbia - initial API and implementation
 *******************************************************************************/

package org.eclipse.mylar.internal.bugzilla.ui.tasklist;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;

import javax.security.auth.login.LoginException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.mylar.context.core.MylarStatusHandler;
import org.eclipse.mylar.internal.bugzilla.core.BugzillaCorePlugin;
import org.eclipse.mylar.internal.bugzilla.core.BugzillaServerFacade;
import org.eclipse.mylar.internal.bugzilla.core.IBugzillaConstants;
import org.eclipse.mylar.internal.bugzilla.core.RepositoryConfiguration;
import org.eclipse.mylar.internal.tasks.core.WebClientUtil;
import org.eclipse.mylar.internal.tasks.ui.wizards.AbstractRepositorySettingsPage;
import org.eclipse.mylar.tasks.core.RepositoryTemplate;
import org.eclipse.mylar.tasks.ui.AbstractRepositoryConnectorUi;
import org.eclipse.mylar.tasks.ui.TasksUiPlugin;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

/**
 * @author Mik Kersten
 */
public class BugzillaRepositorySettingsPage extends AbstractRepositorySettingsPage {

	public static final String LABEL_AUTOMATIC_VERSION = "Automatic (Use Validate Settings)";

	private static final String MESSAGE_FAILURE_UNKNOWN = "Unknown error occured. Check that server url and credentials are valid.";

	private static final String TITLE = "Bugzilla Repository Settings";

	private static final String DESCRIPTION = "Example: https://bugs.eclipse.org/bugs (do not include index.cgi)";

	protected Combo repositoryVersionCombo;

	private boolean testing = false;

	public BugzillaRepositorySettingsPage(AbstractRepositoryConnectorUi repositoryUi) {
		super(TITLE, DESCRIPTION, repositoryUi);
		setNeedsAnonymousLogin(true);
		setNeedsEncoding(true);
		setNeedsTimeZone(false);
	}

	protected void createAdditionalControls(Composite parent) {

		for (RepositoryTemplate template : connector.getTemplates()) {
			repositoryLabelCombo.add(template.label);
		}
		repositoryLabelCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String text = repositoryLabelCombo.getText();
				RepositoryTemplate template = connector.getTemplate(text);
				if (template != null) {
					setUrl(template.repositoryUrl);
					// setAnonymous(info.anonymous);
					setBugzillaVersion(template.version);
					getContainer().updateButtons();
					return;
				}
			}
		});

		Label repositoryVersionLabel = new Label(parent, SWT.NONE);
		repositoryVersionLabel.setText("Repository Version: ");
		repositoryVersionCombo = new Combo(parent, SWT.READ_ONLY);

		repositoryVersionCombo.add(LABEL_AUTOMATIC_VERSION);

		for (String version : getConnector().getSupportedVersions()) {
			repositoryVersionCombo.add(version);
		}
		if (repository != null && repositoryVersionCombo.indexOf(repository.getVersion()) >= 0) {
			repositoryVersionCombo.select(repositoryVersionCombo.indexOf(repository.getVersion()));
		} else {
			int defaultIndex = getConnector().getSupportedVersions().size() - 1;
			repositoryVersionCombo.select(defaultIndex);
			setVersion(repositoryVersionCombo.getItem(defaultIndex));
		}

		repositoryVersionCombo.addSelectionListener(new SelectionListener() {

			public void widgetSelected(SelectionEvent e) {
				if (repositoryVersionCombo.getSelectionIndex() >= 0) {
					setVersion(repositoryVersionCombo.getItem(repositoryVersionCombo.getSelectionIndex()));
				}
			}

			public void widgetDefaultSelected(SelectionEvent e) {
				// ignore
			}
		});
	}

	public void setBugzillaVersion(String version) {
		if (version == null) {
			repositoryVersionCombo.select(0);
		} else {
			int i = repositoryVersionCombo.indexOf(version.toString());
			if (i != -1) {
				repositoryVersionCombo.select(i);
				setVersion(version);
			} else {
				for (IBugzillaConstants.BugzillaServerVersion serverVersion : IBugzillaConstants.BugzillaServerVersion
						.values()) {
					if (version.startsWith(serverVersion.toString())) {
						i = repositoryVersionCombo.indexOf(serverVersion.toString());
						if (i != -1) {
							repositoryVersionCombo.select(i);
							setVersion(serverVersion.toString());
							break;
						}
					}
				}
				if (i == -1) {
					MylarStatusHandler.log("Could not resolve repository version: " + version, this);
					setVersion(IBugzillaConstants.BugzillaServerVersion.SERVER_218.toString());
				}
			}
		}
	}

	@Override
	public boolean isPageComplete() {
		return super.isPageComplete();
	}

	protected boolean isValidUrl(String name) {
		if (name.startsWith(URL_PREFIX_HTTPS) || name.startsWith(URL_PREFIX_HTTP)) {
			try {
				new URL(name);
				return true;
			} catch (MalformedURLException e) {
			}
		}
		return false;
	}

	/* public for testing */
	public void validateSettings() {

		try {
			final URL serverURL = new URL(super.getServerUrl());
			final String serverUrl = getServerUrl();
			final String newUserId = getUserName();
			final String newPassword = getPassword();
			final boolean isAnonymous = isAnonymousAccess();
			final String newEncoding = getCharacterEncoding();
			final boolean checkVersion = repositoryVersionCombo.getSelectionIndex() == 0;
			final String[] version = new String[1];
			getWizard().getContainer().run(true, false, new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					monitor.beginTask("Validating server settings", IProgressMonitor.UNKNOWN);
					try {

						// TODO: all of this should be encapsulated by
						// validateCredentials

						Proxy proxySettings = TasksUiPlugin.getDefault().getProxySettings();
						URLConnection cntx = WebClientUtil.getUrlConnection(serverURL, proxySettings, false);
						if (cntx == null || !(cntx instanceof HttpURLConnection)) {
							throw new MalformedURLException();
						}

						HttpURLConnection serverConnection = (HttpURLConnection) cntx;
						serverConnection.connect();
						serverConnection.getInputStream();

						if (!isAnonymous) {
							BugzillaServerFacade.validateCredentials(proxySettings, serverUrl, newUserId, newPassword);
						}

						if (checkVersion) {
							RepositoryConfiguration config = BugzillaCorePlugin.getRepositoryConfiguration(true,
									repository.getUrl(), proxySettings, newUserId, newPassword, newEncoding);

							if (config != null) {
								version[0] = config.getInstallVersion();
							}
						}

					} catch (Exception e) {
						throw new InvocationTargetException(e);
					} finally {
						monitor.done();
					}
				}
			});

			if (version[0] != null) {
				setBugzillaVersion(version[0]);
			}

			if (!testing) {
				MessageDialog.openInformation(null, IBugzillaConstants.TITLE_MESSAGE_DIALOG,
						"Authentication credentials are valid.");
			}
		} catch (InvocationTargetException e) {
			if (e.getCause() instanceof MalformedURLException) {
				MessageDialog.openWarning(null, IBugzillaConstants.TITLE_MESSAGE_DIALOG, "Server URL is invalid.");
			} else if (e.getCause() instanceof LoginException) {
				MessageDialog.openWarning(null, IBugzillaConstants.TITLE_MESSAGE_DIALOG,
						"Unable to authenticate with server. Login credentials invalid.");
			} else if (e.getCause() instanceof IOException) {
				MessageDialog.openWarning(null, IBugzillaConstants.TITLE_MESSAGE_DIALOG,
						"No Bugzilla server found at url");
			} else {
				MessageDialog.openWarning(null, IBugzillaConstants.TITLE_MESSAGE_DIALOG, MESSAGE_FAILURE_UNKNOWN);
			}
		} catch (MalformedURLException e) {
			MessageDialog.openWarning(null, IBugzillaConstants.TITLE_MESSAGE_DIALOG, "Server URL is invalid.");
		} catch (InterruptedException e) {
			MessageDialog.openWarning(null, IBugzillaConstants.TITLE_MESSAGE_DIALOG, MESSAGE_FAILURE_UNKNOWN);
		}

		super.getWizard().getContainer().updateButtons();
	}

	public void setTesting(boolean testing) {
		this.testing = testing;
	}

}