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
import org.mmtk.utility.deque.*;
import org.mmtk.plan.generational.GenNurseryTraceLocal;
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
@Uninterruptible public abstract class G1NurseryCollector extends StopTheWorldCollector {

  /*****************************************************************************
   * Instance fields
   */

  /**
   *
   */
  protected final G1NurseryTraceLocal nurseryTrace;
  protected final LargeObjectLocal los;
  protected  ObjectReferenceDeque modbuf;
  protected  AddressDeque remset;
  protected  AddressPairDeque arrayRemset;

  public G1NurseryCollector() {
    los = new LargeObjectLocal(Plan.loSpace);
    // arrayRemset = new AddressPairDeque(global().arrayRemsetPool);
    // remset = new AddressDeque("remset", global().remsetPool);
    // modbuf = new ObjectReferenceDeque("modbuf", global().modbufPool);
    nurseryTrace = new G1NurseryTraceLocal(global().nurseryTrace, this);
  }

  @Override
  @NoInline
  public void collectionPhase(short phaseId, boolean primary) {
    if (phaseId == G1GC.PREPARE) {
      los.prepare(true);
      // global().arrayRemsetPool.prepareNonBlocking();
      // global().remsetPool.prepareNonBlocking();
      // global().modbufPool.prepareNonBlocking();
      nurseryTrace.prepare();
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
      if (global().isCurrentGCNursery()) {
        nurseryTrace.completeTrace();
      }
      return;
    }

    if (phaseId == G1GC.RELEASE) {
      los.release(true);
      if (global().isCurrentGCNursery()) {
        nurseryTrace.release();
        // global().arrayRemsetPool.reset();
        // global().remsetPool.reset();
        // global().modbufPool.reset();
      }
      return;
    }

    super.collectionPhase(phaseId, primary);
  }

  @Inline
  private static G1GC global() {
    return (G1GC) VM.activePlan.global();
  }

  @Override
  public TraceLocal getCurrentTrace() {
    if(global().isCurrentGCNursery())
        return nurseryTrace;
    return super.getCurrentTrace();
  }
}
