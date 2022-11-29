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
package org.mmtk.plan.generational;

import org.mmtk.plan.TraceLocal;
import org.mmtk.plan.Trace;
import org.mmtk.utility.HeaderByte;
import org.mmtk.utility.deque.*;

import org.mmtk.vm.VM;

import org.vmmagic.unboxed.*;
import org.vmmagic.pragma.*;

/**
 * This abstract class implements the core functionality for a transitive
 * closure over the heap graph.
 */
@Uninterruptible
public abstract class G1MatureTraceLocal extends TraceLocal {

  /****************************************************************************
   *
   * Instance fields.
   */

  /**
   *
   */
  private final ObjectReferenceDeque modbuf;
  private final AddressDeque remset;
  private final AddressPairDeque arrayRemset;

  /****************************************************************************
   *
   * Initialization
   */

  /**
   *
   * @param specializedScan the id of the specialized scan
   * @param trace the global trace class to use
   * @param plan the state of the generational collector
   */
  public G1MatureTraceLocal(int specializedScan, Trace trace, GenCollector plan) {
    super(specializedScan, trace);
    this.modbuf = plan.modbuf;
    this.remset = plan.remset;
    this.arrayRemset = plan.arrayRemset;
  }

  /**
   * @param trace the global trace class to use
   * @param plan the state of the generational collector
   */
  public G1MatureTraceLocal(Trace trace, GenCollector plan) {
    super(Gen.SCAN_MATURE, trace);
    this.modbuf = plan.modbuf;
    this.remset = plan.remset;
    this.arrayRemset = plan.arrayRemset;
  }

  /****************************************************************************
   *
   * Object processing and tracing
   */

  /**
   * {@inheritDoc}
   */
  @Override
  @Inline
  public boolean isLive(ObjectReference object) {
    if (VM.VERIFY_ASSERTIONS) VM.assertions._assert(!object.isNull());
    if (Gen.inNursery(object)) {
      return Gen.nurserySpace.isLive(object);
    }
    return super.isLive(object);
  }

  /**
   * Return {@code true} if this object is guaranteed not to move during this
   * collection (i.e. this object is definitely not an unforwarded
   * object).
   *
   * @param object the object that might move
   * @return {@code true} if this object is guaranteed not to move during this
   *         collection.
   */
  @Override
  public boolean willNotMoveInCurrentCollection(ObjectReference object) {
    if (Gen.inNursery(object))
      return false;
    else
      return super.willNotMoveInCurrentCollection(object);
  }

  @Override
  @Inline
  public ObjectReference traceObject(ObjectReference object) {
    if (VM.VERIFY_ASSERTIONS) VM.assertions._assert(global().traceFullHeap());
    if (object.isNull()) return object;

    if(G1GC.inNursery(object))
      return G1GC.nurserySpace.traceObject(this, object, G1GC.ALLOC_SURVIVOR);

    if(G1GC.inSurvivor(object))
      return G1GC.survivorSpace.traceObject(this, object, G1GC.ALLOC_MATURE);

    if (Space.isInSpace(G1GC.MS0, object))
      return G1GC.matureSpace0.traceObject(this, object, G1GC.ALLOC_MATURE);
    if (Space.isInSpace(G1GC.MS1, object))
      return G1GC.matureSpace0.traceObject(this, object, G1GC.ALLOC_MATURE);

    return object;
  }
}
