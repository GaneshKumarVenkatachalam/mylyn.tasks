/*******************************************************************************
 * Copyright (c) 2013 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/

package org.eclipse.mylyn.internal.tasks.index.tests;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Random;

import org.eclipse.mylyn.tasks.core.ITask;
import org.junit.Test;

public class TaskListIndexCorruptTest extends AbstractTaskListIndexTest {

	private void setupCorruptIndex() throws Exception {
		setupIndex();
		context.createLocalTask();
		index.waitUntilIdle();

		index.close();
		disposeIndex();

		corruptIndex();
	}

	private void corruptIndex() throws IOException {
		File[] files = tempDir.listFiles();
		if (files == null || files.length == 0) {
			throw new IllegalStateException("index has no files");
		}
		for (File file : files) {
			if (file.isFile()) {
				corruptFile(file);
			}
		}
	}

	private void corruptFile(File file) throws IOException {
		Random random = new Random(123);
		long length = file.length();
		OutputStream stream = new FileOutputStream(file);
		try {
			byte[] bytes = new byte[1];
			for (long i = 0; i < length; ++i) {
				random.nextBytes(bytes);
				stream.write(bytes[0]);
			}
		} finally {
			stream.close();
		}
	}

	@Test
	public void testCorrupt() throws Exception {
		setupCorruptIndex();
		setupIndex();

		ITask task = context.createLocalTask();

		index.waitUntilIdle();

		assertTrue(index.matches(task, task.getSummary()));
	}
}
