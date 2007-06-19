/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.ivy.core.sort;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.resolve.IvyNode;
import org.apache.ivy.plugins.circular.CircularDependencyException;
import org.apache.ivy.plugins.circular.CircularDependencyStrategy;
import org.apache.ivy.plugins.version.VersionMatcher;

public class SortEngine {

    private CircularDependencyStrategy circularStrategy;

    private VersionMatcher versionMatcher;

    public SortEngine() {
    }

    
    public void setCircularDependencyStrategy(CircularDependencyStrategy circularStrategy) {
        this.circularStrategy = circularStrategy;
    }

    public void setVersionMatcher(VersionMatcher versionMatcher) {
        this.versionMatcher = versionMatcher;
    }


    public List sortNodes(Collection nodes) throws CircularDependencyException {
        /*
         * here we want to use the sort algorithm which work on module descriptors : so we first put
         * dependencies on a map from descriptors to dependency, then we sort the keySet (i.e. a
         * collection of descriptors), then we replace in the sorted list each descriptor by the
         * corresponding dependency
         */

        Map dependenciesMap = new LinkedHashMap();
        List nulls = new ArrayList();
        for (Iterator iter = nodes.iterator(); iter.hasNext();) {
            IvyNode node = (IvyNode) iter.next();
            if (node.getDescriptor() == null) {
                nulls.add(node);
            } else {
                List n = (List) dependenciesMap.get(node.getDescriptor());
                if (n == null) {
                    n = new ArrayList();
                    dependenciesMap.put(node.getDescriptor(), n);
                }
                n.add(node);
            }
        }
        List list = sortModuleDescriptors(dependenciesMap.keySet(),
            new SilentNonMatchingVersionReporter());
        List ret = new ArrayList((int) (list.size() * 1.3 + nulls.size())); // attempt to adjust the
        // size to avoid too much list resizing
        for (int i = 0; i < list.size(); i++) {
            ModuleDescriptor md = (ModuleDescriptor) list.get(i);
            List n = (List) dependenciesMap.get(md);
            ret.addAll(n);
        }
        ret.addAll(0, nulls);
        return ret;
    }

    /**
     * Sorts the given ModuleDescriptors from the less dependent to the more dependent. This sort
     * ensures that a ModuleDescriptor is always found in the list before all ModuleDescriptors
     * depending directly on it.
     * 
     * @param moduleDescriptors
     *            a Collection of ModuleDescriptor to sort
     * @param nonMatchingVersionReporter
     *            Used to report some non matching version (when a modules depends on a specific
     *            revision of an other modules present in the of modules to sort with a different
     *            revision.
     * @return a List of sorted ModuleDescriptors
     * @throws CircularDependencyException
     *             if a circular dependency exists
     */
    public List sortModuleDescriptors(Collection moduleDescriptors,
            NonMatchingVersionReporter nonMatchingVersionReporter)
            throws CircularDependencyException {
        ModuleDescriptorSorter sorter = new ModuleDescriptorSorter(moduleDescriptors,
                versionMatcher, nonMatchingVersionReporter, circularStrategy);
        return sorter.sortModuleDescriptors();
    }

}
