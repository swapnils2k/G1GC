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

@Uninterruptible public class G1SurvivorMutator extends G1NurseryMutator {

  protected final CopyLocal survivor = new CopyLocal(G1Survivor.nurserySpace);

  private final ObjectReferenceDeque modbuf;   
  protected final WriteBuffer remset;          
  protected final AddressPairDeque arrayRemset;

  public G1SurvivorMutator() {
    modbuf = new ObjectReferenceDeque("modbuf", global().s_modbufPool);
    remset = new WriteBuffer(global().s_remsetPool);
    arrayRemset = new AddressPairDeque(global().s_arrayRemsetPool);
  }

  
  @Override
  @Inline
  public Address alloc(int bytes, int align, int offset, int allocator, int site) {
    if (allocator == G1Survivor.ALLOC_SURVIVOR) {
      return survivor.alloc(bytes, align, offset);
    }
    return super.alloc(bytes, align, offset, allocator, site);
  }

  @Override
  @Inline
  public void postAlloc(ObjectReference ref, ObjectReference typeRef, int bytes, int allocator) {
    if (allocator != G1Survivor.ALLOC_SURVIVOR) {
      super.postAlloc(ref, typeRef, bytes, allocator);
    }
  }

  @Override
  public Allocator getAllocatorFromSpace(Space space) {
    if (space == G1Survivor.survivorSpace) 
      return survivor;

    return super.getAllocatorFromSpace(space);
  }

  @Inline
  private void fastPath(ObjectReference src, Address slot, ObjectReference tgt, int mode) {
      if (!G1Survivor.inSurvivor(slot) && G1Survivor.inSurvivor(tgt)) {
          remset.insert(slot);
      }
  }

  @Inline
  private void fastPath(Address slot, ObjectReference tgt) {
    if (G1Survivor.inSurvivor(tgt)) {
      remset.insert(slot);
    }
  }


  @Inline
  @Override
  public final boolean objectReferenceBulkCopy(ObjectReference src, Offset srcOffset, ObjectReference dst, Offset dstOffset, int bytes) {
    if (!G1Survivor.inSurvivor(dst)) {
      Address start = dst.toAddress().plus(dstOffset);
      arrayRemset.insert(start, start.plus(bytes));
    }
    return false;
  }

  @Override
  @NoInline
  public void collectionPhase(short phaseId, boolean primary) {
    if (phaseId == G1GC.PREPARE) {
      if(global().isCurrentGCSurvivor()) 
        survivor.reset();
        
      if (global().traceFullHeap()) {
        super.collectionPhase(phaseId, primary);
        modbuf.flushLocal();
        remset.flushLocal();
        arrayRemset.flushLocal();
      } 
      return;
    }

    if (phaseId == G1GC.RELEASE) {
      if (global().traceFullHeap()) {
        super.collectionPhase(phaseId, primary);
      }
      flushRememberedSets();
      assertRemsetsFlushed();
      return;
    }

    super.collectionPhase(phaseId, primary);
  }

  @Inline
  private static G1GC global() {
    return (G1GC) VM.activePlan.global();
  }
}
