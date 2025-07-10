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
package or.lotus.v8.src.debug;

import or.lotus.v8.src.V8Object;
import or.lotus.v8.src.debug.DebugHandler.DebugEvent;
import or.lotus.v8.src.debug.EventData;
import or.lotus.v8.src.debug.ExecutionState;

public interface BreakHandler {

    public void onBreak(DebugEvent type, ExecutionState state, EventData eventData, V8Object data);

}
