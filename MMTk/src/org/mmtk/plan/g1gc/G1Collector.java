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
import org.mmtk.utility.Log;

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
      Log.write("\nAlloc copy function invoked for G1Collector with allocator id ", allocator);

      if (allocator == Plan.ALLOC_LOS) {
        Log.write("\nSince allocator is of type Plan.ALLOC_LOS, we are allocating date to los space");
        if (VM.VERIFY_ASSERTIONS) 
            VM.assertions._assert(Allocator.getMaximumAlignedSize(bytes, align) > Plan.MAX_NON_LOS_COPY_BYTES);
        
        // Log.write("\nAllocating to los space as assertion verified");
        return los.alloc(bytes, align, offset);
      } 

      if(allocator == G1.ALLOC_MATURE) {
          Log.write("\nSince allocator is of type G1.ALLOC_MATURE, we are allocating date to mature space");
          Log.write("\nChecking Assertion condition bytes <= Plan.MAX_NON_LOS_COPY_BYTES ");
          Log.write(bytes <= Plan.MAX_NON_LOS_COPY_BYTES);
          if (VM.VERIFY_ASSERTIONS) 
            VM.assertions._assert(bytes <= Plan.MAX_NON_LOS_COPY_BYTES);
            
          Log.write("\nAllocating to mature space as assertion verified");
          return mature.alloc(bytes, align, offset);
      }
        
      return super.allocCopy(original, bytes, align, offset, allocator);
  }

  @Override
  @Inline
  public final void postCopy(ObjectReference object, ObjectReference typeRef, int bytes, int allocator) {
    ForwardingWord.clearForwardingBits(object);
    if (allocator == Plan.ALLOC_LOS)
      Plan.loSpace.initializeHeader(object, false);
  }

  @Override
  public void collectionPhase(short phaseId, boolean primary) {
    Log.write("\nG1 Collector Processing phase id ", phaseId);
    Log.write(" . Value of primary - ");
    Log.write(primary);
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
    Log.write("\nG1 Collector Done Processing phase id ", phaseId);
  }

  private static G1 global() {
    return (G1) VM.activePlan.global();
  }

  @Override
  public final TraceLocal getCurrentTrace() {
    TraceLocal currentTrace = super.getCurrentTrace();
    if(currentTrace == null) {
      global().nextGCFullHeap = true;
      return matureTrace;
    }
    return currentTrace;
  }
}
