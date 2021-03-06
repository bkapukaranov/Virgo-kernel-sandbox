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

package org.eclipse.virgo.kernel.deployer.test;

import static org.junit.Assert.assertNotNull;

import java.io.File;

import org.eclipse.virgo.kernel.deployer.core.DeploymentException;
import org.eclipse.virgo.kernel.deployer.core.DeploymentIdentity;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.Version;

import org.eclipse.virgo.kernel.install.artifact.InstallArtifactLifecycleListener;
import org.eclipse.virgo.kernel.model.Artifact;
import org.eclipse.virgo.kernel.model.RuntimeArtifactRepository;

/**
 * Test the interactions between the Runtime Artifact Model (RAM) and the deployer.
 * 
 */
public class RAMDeploymentIntegrationTests extends AbstractDeployerIntegrationTest {

    private ServiceRegistration dummyModuleDeployerServiceRegistration;

    private StubInstallArtifactLifecycleListener lifecycleListener;

    private ServiceRegistration lifecycleListenerRegistration;

    private DeploymentIdentity deploymentIdentity;

    private ServiceReference ramReference;

    private RuntimeArtifactRepository ram;

    @Before
    public void setUp() throws Exception {        
        this.lifecycleListener = new StubInstallArtifactLifecycleListener();
        this.lifecycleListenerRegistration = this.kernelContext.registerService(InstallArtifactLifecycleListener.class.getName(),
            this.lifecycleListener, null);

        this.ramReference = this.kernelContext.getServiceReference(RuntimeArtifactRepository.class.getName());
        this.ram = (RuntimeArtifactRepository) this.kernelContext.getService(ramReference);
    }

    @After
    public void tearDown() throws Exception {
        if (this.dummyModuleDeployerServiceRegistration != null) {
            this.dummyModuleDeployerServiceRegistration.unregister();
        }
        if (this.lifecycleListenerRegistration != null) {
            this.lifecycleListenerRegistration.unregister();
        }
        if (this.ramReference != null) {
            this.ram = null;
            this.kernelContext.ungetService(this.ramReference);
            this.ramReference = null;
        }
    }

    @Test
    public void testRAMUndeployment() throws Exception {
        File file = new File("src/test/resources/dummy.jar");
        this.lifecycleListener.assertLifecycleCounts(0, 0, 0, 0);
        this.deploymentIdentity = this.deployer.deploy(file.toURI());
        this.lifecycleListener.assertLifecycleCounts(1, 1, 0, 0);
        Artifact artifact = this.ram.getArtifact(this.deploymentIdentity.getType(), this.deploymentIdentity.getSymbolicName(), new Version(this.deploymentIdentity.getVersion()));
        assertNotNull(artifact);
        artifact.uninstall();
        this.lifecycleListener.assertLifecycleCounts(1, 1, 1, 1);
    }
    
    @Test(expected=DeploymentException.class)
    public void testRAMUndeploymentFollowedByDeployerUndeployment() throws Exception {
        File file = new File("src/test/resources/dummy.jar");
        this.deploymentIdentity = this.deployer.deploy(file.toURI());
        Artifact artifact = this.ram.getArtifact(this.deploymentIdentity.getType(), this.deploymentIdentity.getSymbolicName(), new Version(this.deploymentIdentity.getVersion()));
        artifact.uninstall();
        
        // The following deployer operation should throw DeploymentException.
        this.deployer.undeploy(this.deploymentIdentity);       
    }

}
