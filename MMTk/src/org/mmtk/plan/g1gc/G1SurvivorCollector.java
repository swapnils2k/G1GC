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

import org.mmtk.plan.*;
import org.mmtk.policy.LargeObjectLocal;
import org.mmtk.policy.CopyLocal;
import org.mmtk.utility.deque.*;
import org.vmmagic.unboxed.Address;
import org.vmmagic.unboxed.ObjectReference;

import org.mmtk.vm.VM;

import org.vmmagic.pragma.*;

/**
 * This abstract class implements <i>per-collector thread</i>
 * behavior and state for <i>generational copying collectors</i>.<p>
 *
 * Specifically, this class defines nursery collection behavior (through
 * <code>nurseryTrace</code> and the <code>collectionPhase</code> method).
 * Per-collector thread remset consumers are instantiated here (used by
 * sub-classes).
 *
 * @see Gen
 * @see GenMutator
 * @see StopTheWorldCollector
 * @see CollectorContext
 */
@Uninterruptible public abstract class G1SurvivorCollector extends G1NurseryCollector {

  /*****************************************************************************
   * Instance fields
   */

  /**
   *
   */
  protected final G1NurseryTraceLocal survivorTrace;
  protected final CopyLocal survivor;
  protected final LargeObjectLocal los;
  // protected final ObjectReferenceDeque modbuf;
  // protected final AddressDeque remset;
  // protected final AddressPairDeque arrayRemset;

  public G1SurvivorCollector() {
    los = new LargeObjectLocal(Plan.loSpace);
    survivor = new CopyLocal(G1Survivor.survivorSpace);
    // arrayRemset = new AddressPairDeque(global().s_arrayRemsetPool);
    // remset = new AddressDeque("remset", global().s_remsetPool);
    // modbuf = new ObjectReferenceDeque("modbuf", global().s_modbufPool);
    survivorTrace = new G1NurseryTraceLocal(global().nurseryTrace, this);
  }

  @Override
  @Inline
  public Address allocCopy(ObjectReference original, int bytes, int align, int offset, int allocator) {
      if (allocator == G1Survivor.ALLOC_SURVIVOR) {
        return survivor.alloc(bytes, align, offset);
      }
      
      return null;
  }

  @Override
  @NoInline
  public void collectionPhase(short phaseId, boolean primary) {
    if(global().isCurrentGCSurvivor() || global().traceFullHeap()) {
      if (phaseId == G1GC.PREPARE) {
        los.prepare(true);
        // global().s_arrayRemsetPool.prepareNonBlocking();
        // global().s_remsetPool.prepareNonBlocking();
        // global().s_modbufPool.prepareNonBlocking();
        survivorTrace.prepare();
        return;
      }

      if (phaseId == Simple.STACK_ROOTS && !global().gcFullHeap) {
        VM.scanning.computeNewThreadRoots(getCurrentTrace());
        return;
      }

      if (phaseId == StopTheWorld.ROOTS) {
        VM.scanning.computeGlobalRoots(getCurrentTrace());
        if ( global().traceFullHeap()) {
          VM.scanning.computeStaticRoots(getCurrentTrace());
        }
        if (Plan.SCAN_BOOT_IMAGE && global().traceFullHeap()) {
          VM.scanning.computeBootImageRoots(getCurrentTrace());
        }
        return;
      }
  
      if (phaseId == G1GC.CLOSURE) {
        if (global().isCurrentGCSurvivor()) {
          survivorTrace.completeTrace();
        }
        return;
      }
  
      if (phaseId == G1GC.RELEASE) {
        los.release(true);
        if (global().isCurrentGCSurvivor()) {
          survivorTrace.release();
          // global().s_arrayRemsetPool.reset();
          // global().s_remsetPool.reset();
          // global().s_modbufPool.reset();
        }
        return;
      }
    }

    super.collectionPhase(phaseId, primary);
  }

  @Inline
  private static G1Survivor global() {
    return (G1Survivor) VM.activePlan.global();
  }

  @Override
  public TraceLocal getCurrentTrace() {
    if(global().isCurrentGCSurvivor())
        return survivorTrace;

    return super.getCurrentTrace();
  }
}
