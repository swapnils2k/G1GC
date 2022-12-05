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

import org.mmtk.vm.VM;

import org.vmmagic.pragma.*;
import org.mmtk.utility.Log;

import org.vmmagic.unboxed.Address;
import org.vmmagic.unboxed.ObjectReference;

@Uninterruptible 
public abstract class G1SurvivorCollector extends G1NurseryCollector {

  protected final G1SurvivorTraceLocal survivorTrace;
  protected final CopyLocal survivor;

  public G1SurvivorCollector() {
    survivor = new CopyLocal(G1Survivor.survivorSpace);
    survivorTrace = new G1SurvivorTraceLocal(global().survivorTrace, this);
  }

  @Override
  @Inline
  public Address allocCopy(ObjectReference original, int bytes, int align, int offset, int allocator) {
      if (allocator == G1Survivor.ALLOC_SURVIVOR) {
        Log.write("\nSince allocator is of type G1Survivor.ALLOC_SURVIVOR, we are allocating date to survivor space");
        return survivor.alloc(bytes, align, offset);
      }

      return super.allocCopy(original, bytes, align, offset, allocator);
  }

  @Override
  @NoInline
  public void collectionPhase(short phaseId, boolean primary) {
    if(global().isCurrentGCSurvivor() || global().traceFullHeap()) {
      if (phaseId == G1.PREPARE) {
          survivorTrace.prepare();
          return;
      }
      
      
      if (phaseId == G1.CLOSURE) {
          survivorTrace.completeTrace();
          return;
      }

      if (phaseId == G1.RELEASE) {
          survivorTrace.release();
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
