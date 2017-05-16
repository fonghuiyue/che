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
package org.eclipse.che.plugin.testing.ide.model.info;

/**
 *
 */
public class TestPassedInfo extends AbstractTestStateInfo {

    public static final TestPassedInfo INSTANCE = new TestPassedInfo();

    private TestPassedInfo() {
    }

    @Override
    public boolean isFinal() {
        return false;
    }

    @Override
    public boolean isInProgress() {
        return false;
    }

    @Override
    public boolean isProblem() {
        return false;
    }

    @Override
    public boolean wasLaunched() {
        return true;
    }

    @Override
    public boolean wasTerminated() {
        return false;
    }

    @Override
    public TestStateDescription getDescription() {
        return TestStateDescription.PASSED;
    }
}
