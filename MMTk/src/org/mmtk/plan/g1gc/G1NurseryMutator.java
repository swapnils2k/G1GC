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

@Uninterruptible public class G1NurseryMutator extends StopTheWorldMutator {

  protected final CopyLocal nursery = new CopyLocal(G1Nursery.nurserySpace);

  private  ObjectReferenceDeque modbuf;   
  protected  WriteBuffer remset;          
  protected  AddressPairDeque arrayRemset;

  public G1NurseryMutator() {
    // modbuf = new ObjectReferenceDeque("modbuf", global().modbufPool);
    // remset = new WriteBuffer(global().remsetPool);
    // arrayRemset = new AddressPairDeque(global().arrayRemsetPool);
  }

  
  @Override
  @Inline
  public Address alloc(int bytes, int align, int offset, int allocator, int site) {
    if (allocator == G1Nursery.ALLOC_NURSERY) {
      return nursery.alloc(bytes, align, offset);
    }
    return super.alloc(bytes, align, offset, allocator, site);
  }

  @Override
  @Inline
  public void postAlloc(ObjectReference ref, ObjectReference typeRef, int bytes, int allocator) {
    if (allocator != G1Nursery.ALLOC_NURSERY) {
      super.postAlloc(ref, typeRef, bytes, allocator);
    }
  }

  @Override
  public Allocator getAllocatorFromSpace(Space space) {
    if (space == G1Nursery.nurserySpace) 
      return nursery;

    return super.getAllocatorFromSpace(space);
  }

  @Inline
  private void fastPath(ObjectReference src, Address slot, ObjectReference tgt, int mode) {
      if (!G1Nursery.inNursery(slot) && G1Nursery.inNursery(tgt)) {
          remset.insert(slot);
      }
  }

  @Override
  @Inline
  public final void objectReferenceWrite(ObjectReference src, Address slot, ObjectReference tgt, Word metaDataA, Word metaDataB, int mode) {
    fastPath(src, slot, tgt, mode);
    VM.barriers.objectReferenceWrite(src, tgt, metaDataA, metaDataB, mode);
  }

  @Inline
  private void fastPath(Address slot, ObjectReference tgt) {
    if (G1Nursery.inNursery(tgt)) {
      remset.insert(slot);
    }
  }

  @Override
  @Inline
  public final void objectReferenceNonHeapWrite(Address slot, ObjectReference tgt, Word metaDataA, Word metaDataB) {
    fastPath(slot, tgt);
    VM.barriers.objectReferenceNonHeapWrite(slot, tgt, metaDataA, metaDataB);
  }

  @Override
  @Inline
  public boolean objectReferenceTryCompareAndSwap(ObjectReference src, Address slot, ObjectReference old, ObjectReference tgt,
      Word metaDataA, Word metaDataB, int mode) {
    boolean result = VM.barriers.objectReferenceTryCompareAndSwap(src, old, tgt, metaDataA, metaDataB, mode);
    if (result)
      fastPath(src, slot, tgt, mode);
    return result;
  }


  @Inline
  @Override
  public boolean objectReferenceBulkCopy(ObjectReference src, Offset srcOffset, ObjectReference dst, Offset dstOffset, int bytes) {
    if (!G1Nursery.inNursery(dst)) {
      Address start = dst.toAddress().plus(dstOffset);
      arrayRemset.insert(start, start.plus(bytes));
    }
    return false;
  }

  @Override
  public final void flushRememberedSets() {
    modbuf.flushLocal();
    remset.flushLocal();
    arrayRemset.flushLocal();
    assertRemsetsFlushed();
  }

  @Override
  public final void assertRemsetsFlushed() {
    if (VM.VERIFY_ASSERTIONS) {
      VM.assertions._assert(modbuf.isFlushed());
      VM.assertions._assert(remset.isFlushed());
      VM.assertions._assert(arrayRemset.isFlushed());
    }
  }

  @Override
  @NoInline
  public void collectionPhase(short phaseId, boolean primary) {

    if (phaseId == G1GC.PREPARE) {
      if(global().isCurrentGCNursery()) 
        nursery.reset();

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
