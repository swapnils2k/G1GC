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
import org.mmtk.utility.Log;
import org.mmtk.utility.deque.*;

import org.mmtk.vm.VM;

import org.vmmagic.pragma.*;


@Uninterruptible 
public abstract class G1NurseryCollector extends StopTheWorldCollector {

  protected final G1NurseryTraceLocal nurseryTrace;
  protected final LargeObjectLocal los;

  public G1NurseryCollector() {
    los = new LargeObjectLocal(Plan.loSpace);
    nurseryTrace = new G1NurseryTraceLocal(global().nurseryTrace, this);
  }

  @Override
  @NoInline
  public void collectionPhase(short phaseId, boolean primary) {
    if(global().isCurrentGCNursery() || global().traceFullHeap()) {
      if (phaseId == G1.PREPARE) {
          nurseryTrace.prepare();
          return;
      }

      if (phaseId == G1.CLOSURE) {
          nurseryTrace.completeTrace();
          return;
      }

      if (phaseId == G1.RELEASE) {
          nurseryTrace.release();
          return;
      }
    }
    
    super.collectionPhase(phaseId, primary);
  }

  @Inline
  private static G1 global() {
    return (G1) VM.activePlan.global();
  }

  @Override
  public TraceLocal getCurrentTrace() {
      if(global().isCurrentGCNursery()) {
        // Log.write("\nReturning nursery trace");
        return nurseryTrace;
      }

      return null;
  }
}
