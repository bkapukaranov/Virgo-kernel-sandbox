/*******************************************************************************
 * Copyright (c) 2008, 2010 VMware Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   VMware Inc. - initial contribution
 *******************************************************************************/

package org.eclipse.virgo.kernel.deployer.core.event;

import org.eclipse.virgo.kernel.osgi.common.Version;


/**
 * {@link ApplicationUndeploying} is an event which is broadcast when an application is about to be undeployed.
 * <p />
 *
 * <strong>Concurrent Semantics</strong><br />
 *
 * This class is immutable and therefore thread safe.
 *
 */
public class ApplicationUndeploying extends ApplicationDeploymentEvent {

    /**
     * Construct a {@link ApplicationUndeploying} event with the given application symbolic name and version.
     * 
     * @param applicationSymbolicName
     * @param applicationVersion
     */
    public ApplicationUndeploying(String applicationSymbolicName, Version applicationVersion) {
        super(applicationSymbolicName, applicationVersion);
    }

}
