/*******************************************************************************
 * Copyright (c) 2012-2017 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.plugin.testing.ide.model.event;

import org.eclipse.che.plugin.testing.ide.messages.TestFailed;

/**
 *
 */
public class TestFailedEvent extends TestNodeEvent {

    private final String failureMessage;
    private final String stackTrace;
    private final boolean error;

    public TestFailedEvent(TestFailed message) {
        super(getNodeId(message), message.getTestName());
        failureMessage = message.getFailureMessage();
        stackTrace = message.getStackTrace();
        error = message.isError();
        //TODO add additional info about failed test
    }

    public String getFailureMessage() {
        return failureMessage;
    }

    public boolean isError() {
        return error;
    }

    public String getStackTrace() {
        return stackTrace;

    }
}
