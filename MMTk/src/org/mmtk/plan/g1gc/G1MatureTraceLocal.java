/*
 *  This file is part of the Jikes RVM project (http://jikesrvm.org).
 *
 *  This file is licensed to You under the Eclipse Public License (EPL);
 *  You may not use this file except in compliance with the License. You
 *  may obtain a copy of the License at
 *
 *      http://www.opensource.org/licenses/eclipse-1.0.php
 *
 *  See the COPYRIGHT.txt file distributed with this work for information
 *  regarding copyright ownership.
 */
package org.mmtk.plan.g1gc;

import org.mmtk.plan.TraceLocal;
import org.mmtk.plan.Trace;
import org.mmtk.utility.HeaderByte;
import org.mmtk.utility.deque.*;

import org.mmtk.vm.VM;

import org.vmmagic.unboxed.*;
import org.vmmagic.pragma.*;

@Uninterruptible
public abstract class G1MatureTraceLocal extends TraceLocal {

  public G1MatureTraceLocal(Trace trace, G1MatureCollector plan) {
    super(G1GC.SCAN_MATURE, trace);
  }

  @Override
  @Inline
  public boolean isLive(ObjectReference object) {
    if (VM.VERIFY_ASSERTIONS) VM.assertions._assert(!object.isNull());
    if (G1GC.inNursery(object)) {
      return G1GC.nurserySpace.isLive(object);
    }

    if (G1GC.inSurvivor(object)) {
      return G1GC.survivorSpace.isLive(object);
    }

    if(G1GC.inMature(object)) {
      return G1GC.toSpace().isLive(object);
    }

    return super.isLive(object);
  }

  @Override
  @Inline
  public ObjectReference traceObject(ObjectReference object) {
    if (VM.VERIFY_ASSERTIONS) VM.assertions._assert(global().traceFullHeap());
    if (object.isNull()) return object;

    if(G1GC.inNursery(object))
      return G1GC.nurserySpace.traceObject(this, object, G1GC.ALLOC_MATURE);

    if(G1GC.inSurvivor(object))
      return G1GC.survivorSpace.traceObject(this, object, G1GC.ALLOC_MATURE);

    if (Space.isInSpace(G1GC.MS0, object))
      return G1GC.matureSpace0.traceObject(this, object, G1GC.ALLOC_MATURE);

    if (Space.isInSpace(G1GC.MS1, object))
      return G1GC.matureSpace0.traceObject(this, object, G1GC.ALLOC_MATURE);

    
    return super.traceObject(object);
  }

  @Inline
  private static G1GC global() {
    return (G1GC) VM.activePlan.global();
  }
}
