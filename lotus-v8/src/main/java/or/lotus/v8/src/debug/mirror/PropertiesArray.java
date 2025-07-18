/*******************************************************************************
 * Copyright (c) 2016 EclipseSource and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    EclipseSource - initial API and implementation
 ******************************************************************************/
package or.lotus.v8.src.debug.mirror;

import or.lotus.v8.src.Releasable;
import or.lotus.v8.src.V8Array;
import or.lotus.v8.src.V8Object;
import or.lotus.v8.src.debug.mirror.PropertyMirror;

/**
 * Provides typed access to a set of properties.
 */
public class PropertiesArray implements Releasable {

    private V8Array v8Array;

    PropertiesArray(final V8Array v8Object) {
        v8Array = v8Object.twin();
    }

    /**
     * Returns the PropertyMiror at a given index.
     *
     * @param index The index of the property
     * @return The property at the given index
     */
    public or.lotus.v8.src.debug.mirror.PropertyMirror getProperty(final int index) {
        V8Object result = v8Array.getObject(index);
        try {
            return new PropertyMirror(result);
        } finally {
            result.close();
        }
    }

    /*
     * (non-Javadoc)
     * @see java.io.Closeable#close()
     */
    @Override
    public void close() {
        if (!v8Array.isReleased()) {
            v8Array.close();
        }
    }

    /*
     * (non-Javadoc)
     * @see or.lotus.v8.src.Releasable#release()
     */
    @Override
    @Deprecated
    public void release() {
        close();
    }

    /**
     * Returns the number of properties contained in this array.
     *
     * @return The length of this array.
     */
    public int length() {
        return v8Array.length();
    }

}
