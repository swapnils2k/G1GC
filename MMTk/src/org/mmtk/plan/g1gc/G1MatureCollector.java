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

import org.mmtk.plan.generational.Gen;
import org.mmtk.plan.generational.GenCollector;
import org.mmtk.plan.Plan;
import org.mmtk.plan.TraceLocal;
import org.mmtk.policy.CopyLocal;
import org.mmtk.utility.ForwardingWord;
import org.mmtk.utility.HeaderByte;
import org.mmtk.utility.alloc.Allocator;
import org.mmtk.vm.VM;

import org.mmtk.plan.generational.copying.GenCopyMatureTraceLocal;

import org.vmmagic.unboxed.*;
import org.vmmagic.pragma.*;

/**
 * This class implements <i>per-collector thread</i> behavior and state for
 * the <code>GenCopy</code> two-generational copying collector.<p>
 *
 * Specifically, this class defines semantics specific to the collection of
 * the mature generation (<code>GenCollector</code> defines nursery semantics).
 * In particular the mature space allocator is defined (for collection-time
 * allocation into the mature space), and the mature space per-collector thread
 * collection time semantics are defined.<p>
 *
 * @see GenCopy for a description of the <code>GenCopy</code> algorithm.
 *
 * @see GenCopy
 * @see GenCopyMutator
 * @see GenCollector
 * @see org.mmtk.plan.StopTheWorldCollector
 * @see org.mmtk.plan.CollectorContext
 */
@Uninterruptible
public class G1MatureCollector extends G1SurvivorCollector {

  /******************************************************************
   * Instance fields
   */

  /** The allocator for the mature space */
  private final CopyLocal mature;

  /** The trace object for full-heap collections */
  private final G1MatureTraceLocal matureTrace;

  /****************************************************************************
   *
   * Initialization
   */

  /**
   * Constructor
   */
  public G1MatureCollector() {
    mature = new CopyLocal(G1GC.toSpace());
    matureTrace = new G1MatureTraceLocal(global().matureTrace, this);
  }

  @Override
  @Inline
  public Address allocCopy(ObjectReference original, int bytes,
      int align, int offset, int allocator) {
    if (allocator == Plan.ALLOC_LOS) {
      if (VM.VERIFY_ASSERTIONS) VM.assertions._assert(Allocator.getMaximumAlignedSize(bytes, align) > Plan.MAX_NON_LOS_COPY_BYTES);
          return los.alloc(bytes, align, offset);
    } else {
      if(allocator == G1GC.ALLOC_MATURE)
        return mature.alloc(bytes, align, offset);
      
      return super.allocCopy(original, bytes, align, offset, allocator);
    }
    
  }


  /*****************************************************************************
   *
   * Collection
   */

  /**
   * {@inheritDoc}
   */
  @Override
  public void collectionPhase(short phaseId, boolean primary) {
    if (global().traceFullHeap()) {
      if (phaseId == G1GC.PREPARE) {
        super.collectionPhase(phaseId, primary);
        if (global().gcFullHeap) 
          mature.rebind(G1GC.toSpace());
      }
      if (phaseId == G1GC.CLOSURE) {
        matureTrace.completeTrace();
        return;
      }
      if (phaseId == G1GC.RELEASE) {
        matureTrace.release();
        super.collectionPhase(phaseId, primary);
        return;
      }
    }
    super.collectionPhase(phaseId, primary);
  }

  /*****************************************************************************
   *
   * Miscellaneous
   */

  /** @return The active global plan as a <code>GenCopy</code> instance. */
  private static G1GC global() {
    return (G1GC) VM.activePlan.global();
  }

  @Override
  public final TraceLocal getCurrentTrace() {
    if(global().traceFullHeap())
        return matureTrace;

    return super.getCurrentTrace();
  }
}
