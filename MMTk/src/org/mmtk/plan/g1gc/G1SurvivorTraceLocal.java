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

import static org.mmtk.utility.Constants.BYTES_IN_ADDRESS;

import org.mmtk.plan.TraceLocal;
import org.mmtk.plan.Trace;
import org.mmtk.utility.HeaderByte;
import org.mmtk.utility.deque.*;
import org.mmtk.vm.VM;

import org.vmmagic.pragma.*;
import org.vmmagic.unboxed.*;

/**
 * This class implements the core functionality for a transitive
 * closure over the heap graph.
 */
@Uninterruptible
public final class G1SurvivorTraceLocal extends TraceLocal {

  private  ObjectReferenceDeque modbuf;
  private  AddressDeque remset;
  private  AddressPairDeque arrayRemset;

  public G1SurvivorTraceLocal(Trace trace, G1SurvivorCollector plan) {
    super(G1GC.SCAN_NURSERY, trace);
  }

  @Override
  public boolean isLive(ObjectReference object) {
    if (object.isNull()) 
      return false;

    if (G1GC.inSurvivor(object)) {
      return G1GC.survivorSpace.isLive(object);
    }

    return true;
  }

  @Override
  @Inline
  public ObjectReference traceObject(ObjectReference object) {
    if (G1GC.inSurvivor(object)) {
      return G1GC.survivorSpace.traceObject(this, object, G1GC.ALLOC_MATURE);
    }

    return object;
  }

  @Override
  public boolean willNotMoveInCurrentCollection(ObjectReference object) {
    if (object.isNull()) return false;
    
    return !G1GC.inSurvivor(object);
  }

}
