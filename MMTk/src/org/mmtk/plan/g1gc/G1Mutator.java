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

import org.mmtk.policy.CopyLocal;
import org.mmtk.policy.Space;
import org.mmtk.utility.alloc.Allocator;
import org.mmtk.vm.VM;
import org.mmtk.utility.Log;

import org.vmmagic.unboxed.*;
import org.vmmagic.pragma.*;

@Uninterruptible
public class G1Mutator extends G1SurvivorMutator {
 
  private final CopyLocal mature;

  public G1Mutator() {
    mature = new CopyLocal();
  }

  @Override
  public void initMutator(int id) {
    super.initMutator(id);
    mature.rebind(G1.toSpace());
  }

  @Override
  @Inline
  public Address alloc(int bytes, int align, int offset, int allocator, int site) {
    // Log.write("\nAllocating with allocator id " , allocator);
    // Log.write("\nByte - " , bytes);
    // Log.write("\nAlign - " , align);
    // Log.write("\nSite - " , site);

    if (allocator == G1.ALLOC_MATURE) {
      // Log.write("\nAllocating into mature");
      return mature.alloc(bytes, align, offset);
    }

    return super.alloc(bytes, align, offset, allocator, site);
  }

  @Override
  @Inline
  public void postAlloc(ObjectReference object, ObjectReference typeRef, int bytes, int allocator) {
    // Log.write("\nPost Allocating with allocator id " , allocator);
    // Log.write("\nByte - " , bytes);
    if (allocator == G1.ALLOC_MATURE) { 
        // Log.write("\nPost allocating into mature");
        return;
    }
        
    super.postAlloc(object, typeRef, bytes, allocator);
  }
  
  @Override
  public Allocator getAllocatorFromSpace(Space space) {
    if (space == G1.matureSpace0 || space == G1.matureSpace1) 
      return mature;

    return super.getAllocatorFromSpace(space);
  }

  @Override
  public void collectionPhase(short phaseId, boolean primary) {
    if (global().traceFullHeap()) {
      if (phaseId == G1.PREPARE) {
          super.collectionPhase(phaseId, primary);
          mature.reset();
          return;
      } 
        
      if (phaseId == G1.RELEASE) {
        super.collectionPhase(phaseId, primary);
        mature.rebind(G1.toSpace());
        return;
      }
    }
  
    super.collectionPhase(phaseId, primary);
  }

  private static G1 global() {
    return (G1) VM.activePlan.global();
  }

}
