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
import org.mmtk.utility.Log;
import org.mmtk.plan.*;
import org.mmtk.policy.CopyLocal;
import org.mmtk.policy.Space;
import org.mmtk.utility.HeaderByte;
import org.mmtk.utility.deque.*;
import org.mmtk.utility.alloc.Allocator;
import org.mmtk.utility.statistics.Stats;
import org.mmtk.vm.VM;
import static org.mmtk.utility.Constants.*;

import org.vmmagic.pragma.*;
import org.vmmagic.unboxed.*;

@Uninterruptible public class G1NurseryMutator extends StopTheWorldMutator {

  protected final CopyLocal nursery = new CopyLocal(G1Nursery.nurserySpace);

  @Override
  @Inline
  public Address alloc(int bytes, int align, int offset, int allocator, int site) {
    if (allocator == G1Nursery.ALLOC_NURSERY) {
      Log.write("\nAllocating in nursery with allocator id " , allocator);
      return nursery.alloc(bytes, align, offset);
    }
    return super.alloc(bytes, align, offset, allocator, site);
  }

  @Override
  @Inline
  public void postAlloc(ObjectReference object, ObjectReference typeRef, int bytes, int allocator) {
    if (allocator == G1Nursery.ALLOC_NURSERY) {
      // Log.write("\nPost allocating into nursery\n");
      return;
    }
      
    super.postAlloc(object, typeRef, bytes, allocator);
  }

  @Override
  public Allocator getAllocatorFromSpace(Space space) {
    if (space == G1Nursery.nurserySpace) 
      return nursery;

    return super.getAllocatorFromSpace(space);
  }

  @Override
  @NoInline
  public void collectionPhase(short phaseId, boolean primary) {
    if (phaseId == G1.PREPARE) {
      if(global().isCurrentGCNursery()) {
        nursery.reset();
        return;
      }

      if(global().traceFullHeap()) {
        super.collectionPhase(phaseId, primary);
        nursery.reset();
        return;
      }
    }

    super.collectionPhase(phaseId, primary);
  }

  @Inline
  private static G1 global() {
    return (G1) VM.activePlan.global();
  }
}
