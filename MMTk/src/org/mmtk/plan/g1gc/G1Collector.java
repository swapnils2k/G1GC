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

import org.mmtk.plan.Plan;
import org.mmtk.plan.TraceLocal;
import org.mmtk.policy.CopyLocal;
import org.mmtk.utility.ForwardingWord;
import org.mmtk.utility.HeaderByte;
import org.mmtk.utility.alloc.Allocator;
import org.mmtk.vm.VM;

import org.vmmagic.unboxed.*;
import org.vmmagic.pragma.*;

@Uninterruptible
public class G1Collector extends G1SurvivorCollector {

  private final CopyLocal mature;
  private final G1MatureTraceLocal matureTrace;
  
  public G1Collector() {
    mature = new CopyLocal(G1.toSpace());
    matureTrace = new G1MatureTraceLocal(global().matureTrace, this);
  }

  @Override
  @Inline
  public Address allocCopy(ObjectReference original, int bytes, int align, int offset, int allocator) {
      if(allocator == G1.ALLOC_MATURE)
          return mature.alloc(bytes, align, offset);
        
      return super.allocCopy(original, bytes, align, offset, allocator);
  }

  @Override
  public void collectionPhase(short phaseId, boolean primary) {
    if (global().traceFullHeap()) {
      if (phaseId == G1.PREPARE) {
          matureTrace.prepare();
          mature.rebind(G1.toSpace());
          return;
      }
      
      if (phaseId == G1.CLOSURE) {
        matureTrace.completeTrace();
        return;
      }

      if (phaseId == G1.RELEASE) {
        matureTrace.release();
        return;
      }
    }

    super.collectionPhase(phaseId, primary);
  }

  private static G1 global() {
    return (G1) VM.activePlan.global();
  }

  @Override
  public final TraceLocal getCurrentTrace() {
    TraceLocal currentTrace = super.getCurrentTrace();
    return currentTrace == null ? matureTrace : currentTrace;
  }
}
