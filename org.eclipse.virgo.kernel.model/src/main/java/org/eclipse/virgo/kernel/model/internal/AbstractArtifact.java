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

package org.eclipse.virgo.kernel.model.internal;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.virgo.kernel.model.Artifact;
import org.eclipse.virgo.kernel.serviceability.NonNull;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.Version;
import org.osgi.util.tracker.ServiceTracker;


/**
 * An abstract implementation of {@link Artifact} that all implementations should extend.
 * <p/>
 * Implements {@link #getDependents()} by delegating to {@link DependencyDeterminer} found in the service registry. The
 * appropriate {@link DependencyDeterminer} is located by finding a a service with the property
 * <code>artifactType</code> with the a value equal to the value of {@link #getType()}.
 * <p />
 * {@link #equals(Object)} and {@link #hashCode()} are also implemented to guarantee that equality is based on the type,
 * name, and version of the artifact.
 * <p/>
 * 
 * <strong>Concurrent Semantics</strong><br />
 * 
 * Threadsafe and all subclasses should be threadsafe
 * 
 * @see DependencyDeterminer
 */
public abstract class AbstractArtifact implements Artifact {

    private static final String FILTER_FORMAT = "(&(objectClass=%s)(artifactType=%s))";

    private final String type;

    private final String name;

    private final Version version;

    private final ServiceTracker dependencyDeterminerTracker;

    public AbstractArtifact(@NonNull BundleContext bundleContext, @NonNull String type, @NonNull String name, @NonNull Version version) {
        this.type = type;
        this.name = name;
        this.version = version;

        Filter filter;
        try {
            filter = bundleContext.createFilter(String.format(FILTER_FORMAT, DependencyDeterminer.class.getCanonicalName(), type));
        } catch (InvalidSyntaxException e) {
            throw new RuntimeException(String.format("Cannot create Service Registry filter for a DependencyDeterminer of type '%s'", type), e);
        }
        this.dependencyDeterminerTracker = new ServiceTracker(bundleContext, filter, null);
        this.dependencyDeterminerTracker.open();
    }

    /**
     * {@inheritDoc}
     */
    public final String getType() {
        return this.type;
    }

    /**
     * {@inheritDoc}
     */
    public final String getName() {
        return this.name;
    }

    /**
     * {@inheritDoc}
     */
    public final Version getVersion() {
        return this.version;
    }

    /**
     * {@inheritDoc}
     */
    public final Set<Artifact> getDependents() {
        Set<Artifact> dependents = new HashSet<Artifact>();

        Object[] dependencyDeterminers = this.dependencyDeterminerTracker.getServices();
        if (dependencyDeterminers != null) {
            for (Object dependencyDeterminer : dependencyDeterminers) {
                dependents.addAll(((DependencyDeterminer) dependencyDeterminer).getDependents(this));
            }
        }

        return Collections.unmodifiableSet(dependents);
    }

    /**
     * {@inheritDoc}
     */
    public Map<String, String> getProperties() {
        return Collections.<String, String> emptyMap();
    }

    /**
     * {@inheritDoc}
     */
    public final int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + name.hashCode();
        result = prime * result + type.hashCode();
        result = prime * result + version.hashCode();
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Artifact)) {
            return false;
        }
        Artifact other = (Artifact) obj;
        if (!name.equals(other.getName())) {
            return false;
        }
        if (!type.equals(other.getType())) {
            return false;
        }
        if (!version.equals(other.getVersion())) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return String.format("type: %s, name: %s, version: %s", this.type, this.name, this.version.toString());
    }
}
