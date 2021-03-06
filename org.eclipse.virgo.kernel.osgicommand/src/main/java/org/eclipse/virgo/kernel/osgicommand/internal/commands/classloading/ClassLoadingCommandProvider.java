/*******************************************************************************
 * Copyright (c) 2010 SAP AG
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Hristo Iliev, SAP AG - initial contribution
 *******************************************************************************/
package org.eclipse.virgo.kernel.osgicommand.internal.commands.classloading;

import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.osgi.framework.console.CommandProvider;
import org.eclipse.virgo.kernel.osgicommand.helper.ClassLoadingHelper;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * Class loading commands for supportability and diagnostics
 *
 * @author Hristo Spaschev Iliev
 * @version 1.0
 */
public class ClassLoadingCommandProvider implements CommandProvider {
    public static final String NEW_LINE = System.getProperty("line.separator", "\n"); //$NON-NLS-1$ //$NON-NLS-2$
    private BundleContext bundleContext;

    public ClassLoadingCommandProvider(BundleContext context) {
        this.bundleContext = context;
    }

    /**
     * Lists all bundles that contain a class
     *
     * @param interpreter CommandInterpreter instance
     */
    public void _clhas(CommandInterpreter interpreter) {
        String className = interpreter.nextArgument();
        if (className == null) {
            interpreter.println("No class name specified");
            return;
        }

        HashMap<Bundle, Bundle> foundBundles = ClassLoadingHelper.getBundlesLoadingClass(bundleContext, className);
        if (foundBundles.size() == 0) {
            interpreter.println("No bundle contains class [" + className + "]");
            return;
        }

        // build a set with all bundles that contain the class, eliminating repeating ones
        HashSet<Bundle> uniqueBundles = new HashSet<Bundle>(foundBundles.values());

        // build output map
        HashMap<Long, String> outputBundles = new HashMap<Long, String>();
        for (Bundle bundle : uniqueBundles) {
            outputBundles.put(bundle.getBundleId(), bundle.getSymbolicName());
        }

        outputFoundBundles(interpreter, "Bundles containing [" + className + "]:", outputBundles);
    }

    /**
     * Lists all bundles that can load a class
     *
     * @param interpreter CommandInterpreter instance
     */
    public void _clload(CommandInterpreter interpreter) {
        String className = interpreter.nextArgument();
        if (className == null) {
            interpreter.println("No class name specified");
            return;
        }
        String bundle = interpreter.nextArgument();

        HashMap<Bundle, Bundle> foundBundles;
        if (bundle == null) {
            foundBundles = ClassLoadingHelper.getBundlesLoadingClass(bundleContext, className);
        } else {
            foundBundles = ClassLoadingHelper.getBundlesLoadingClass(bundleContext, className, bundle);
        }

        if (foundBundles.size() == 0) {
            if (bundle == null) {
                interpreter.println("No bundle can load class [" + className + "]");
            } else {
                interpreter.println("Bundle [" + bundle + "] cannot load class [" + className + "]");
            }
            return;
        }

        outputFoundBundlesAndRelations(interpreter, "Successfully loaded [" + className + "] " + ((bundle != null) ? "using class loader from:" : "from:"), foundBundles, "exported by");
    }

    /**
     * Lists all bundles that export a class
     *
     * @param interpreter CommandInterpreter instance
     */
    public void _clexport(CommandInterpreter interpreter) {
        String className = interpreter.nextArgument();
        if (className == null) {
            interpreter.println("No class name specified");
            return;
        }

        // Check if the class has a package
        int index = className.lastIndexOf(".");
        if (index == -1) {
            interpreter.println("The class name [" + className + "] contains no package");
            return;
        }
        String classPackage = className.substring(0, index);

        Bundle[] bundles = bundleContext.getBundles();
        HashMap<Long, String> foundBundles = new HashMap<Long, String>();
        for (Bundle bundle : bundles) {
            if (ClassLoadingHelper.isPackageExported(bundleContext, classPackage, bundle)) {
                if (ClassLoadingHelper.tryToLoadClass(className, bundle) != null) {
                    foundBundles.put(bundle.getBundleId(), bundle.getSymbolicName());
                } else {
                    foundBundles.put(bundle.getBundleId(), bundle.getSymbolicName() + "     [class not found, package only]");
                }
            }
        }

        if (foundBundles.size() == 0) {
            interpreter.println("No bundle exports class [" + className + "]");
            return;
        }

        outputFoundBundles(interpreter, "Bundles exporting [" + className + "]:", foundBundles);
    }

    /**
     * Outputs a list with all found bundles
     *
     * @param interpreter  CommandInterpreter instance for output to the console
     * @param message      Message to print before the list
     * @param foundBundles A map with ID and bundle details
     */
    private void outputFoundBundles(CommandInterpreter interpreter, String message, HashMap<Long, String> foundBundles) {
        interpreter.println();
        interpreter.println(message);
        for (Map.Entry<Long, String> entry : foundBundles.entrySet()) {
            interpreter.println("  " + entry.getKey() + "\t" + entry.getValue());
        }
    }

    /**
     * Outputs a list with all found bundles
     *
     * @param interpreter  CommandInterpreter instance for output to the console
     * @param message      Message to print before the list
     * @param foundBundles A map with ID and bundle details
     * @param relation     Relation between the bundles
     */
    private void outputFoundBundlesAndRelations(CommandInterpreter interpreter, String message, HashMap<Bundle, Bundle> foundBundles, String relation) {
        interpreter.println();
        interpreter.println(message);
        for (Map.Entry<Bundle, Bundle> entry : foundBundles.entrySet()) {
            Bundle testBundle = entry.getKey();
            Bundle originalBundle = entry.getValue();
            if (testBundle.equals(originalBundle)) {
                interpreter.println("  " + testBundle.getBundleId() + "\t" + testBundle.getSymbolicName());
            } else {
                interpreter.println("  " + testBundle.getBundleId() + "\t" + testBundle.getSymbolicName());
                if (relation != null)
                    interpreter.println("  " + "\t\t[" + relation + " " + originalBundle.getBundleId() + " " + originalBundle.getSymbolicName() + "]");
            }
        }
    }

    public String getHelp() {
        StringBuffer help = new StringBuffer();
        help.append("---");
        help.append("Classloading Commands");
        help.append("---");
        help.append(NEW_LINE);
        help.append("\tclhas <class name> - lists all bundles that contain a class with the specified name.").append(NEW_LINE);
        help.append("\tclload <class name> [<bundle id> | <bundle name>]- lists all bundles that can load a class or tries to load the class with specified bundle.").append(NEW_LINE);
        help.append("\tclexport <class name> - lists all bundles that export a class with the specified name.").append(NEW_LINE);
        return help.toString();
    }

}

