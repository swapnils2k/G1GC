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

import static org.mmtk.utility.Conversions.pagesToBytes;

import org.mmtk.plan.*;
import org.mmtk.policy.CopySpace;
import org.mmtk.policy.Space;

import org.mmtk.utility.deque.*;
import org.mmtk.utility.heap.VMRequest;
import org.mmtk.utility.heap.layout.HeapLayout;
import org.mmtk.utility.Log;
import org.mmtk.utility.options.Options;
import org.mmtk.utility.sanitychecker.SanityChecker;
import org.mmtk.utility.statistics.*;

import org.mmtk.vm.VM;

import org.vmmagic.pragma.*;
import org.vmmagic.unboxed.*;

@Uninterruptible
public class G1Nursery extends StopTheWorld {

  // Allocators
  public static final int ALLOC_NURSERY        = ALLOC_DEFAULT;
  public static final int SCAN_NURSERY         = 0;

  /* The nursery space is where all new objects are allocated by default */
  public static final boolean IGNORE_REMSETS = true;

  /** Fraction of available virtual memory to give to the nursery (if contiguous) */
  protected static final float NURSERY_VM_FRACTION = 0.2f;
  public static final VMRequest vmRequest = VMRequest.highFraction(NURSERY_VM_FRACTION);
  public static final CopySpace nurserySpace = new CopySpace("nursery", false, vmRequest);
  public static final int NURSERY = nurserySpace.getDescriptor();
  public static final Address NURSERY_START = nurserySpace.getStart();
  
  /* The trace object */
  public final Trace nurseryTrace = new Trace(metaDataSpace);

  public boolean nextGCNursery;
  public boolean nextGCSurvivor;
  public boolean nextGCFullHeap;

  public boolean gcNursery;
  public boolean gcSurvivor;
  public boolean gcFullHeap;

  @Override
  @NoInline
  public void collectionPhase(short phaseId) {
    if (phaseId == SET_COLLECTION_KIND) {
      super.collectionPhase(phaseId);
      gcNursery = nextGCNursery;
      gcSurvivor = nextGCSurvivor;
      gcFullHeap = nextGCFullHeap;
      return;
    }

    if (phaseId == PREPARE) {
      if(isCurrentGCNursery()) {
        nurserySpace.prepare(true);
        return;
      }

      if(traceFullHeap()) {
        super.collectionPhase(phaseId);
        nurserySpace.prepare(true);
        return;
      }
    }

    if (phaseId == CLOSURE) {
      if(isCurrentGCNursery()) {
        nurseryTrace.prepare();
        return;
      }

      if(traceFullHeap()) {
        super.collectionPhase(phaseId);
        nurseryTrace.prepare();
        return;
      }
    }

    if (phaseId == RELEASE) {
      if(isCurrentGCNursery()) {
        nurserySpace.release();
        nurseryTrace.release();
      }

      if (traceFullHeap()) {
        nurserySpace.release();
        nurseryTrace.release();
        super.collectionPhase(phaseId);
      }

      nextGCNursery = false;
      gcNursery = false;
      return;
    }

    super.collectionPhase(phaseId);
  }

  @Override
  public boolean collectionRequired(boolean spaceFull, Space space) {
      if(space == nurserySpace && spaceFull) {
          Log.write("\nSince space object is equal to nursery space, setting nextGCNursery as true");
          nextGCNursery = true;
          return true;
      }

      return super.collectionRequired(spaceFull, space);
  }
 
  @Override
  public int getPagesUsed() {
    return (nurserySpace.reservedPages() + super.getPagesUsed());
  }

  @Override
  public int getCollectionReserve() {
    return nurserySpace.reservedPages() + super.getCollectionReserve();
  }

  @Override
  public int getPagesAvail() {
    return super.getPagesAvail() >> 1;
  }
  
  @Inline
  static boolean inNursery(Address addr) {
      return addr.GE(NURSERY_START);
  }

  @Inline
  static boolean inNursery(ObjectReference obj) {
    return inNursery(obj.toAddress());
  }

  public final boolean traceFullHeap() {
    return (!gcNursery && !gcSurvivor) || gcFullHeap;
  }

  @Override
  public final boolean isCurrentGCNursery() {
    return gcNursery && !gcFullHeap;
  }

  public final boolean isCurrentGCSurvivor() {
    return gcSurvivor && !gcFullHeap;
  }

  @Override
  @Interruptible
  public void registerSpecializedMethods() {
    TransitiveClosure.registerSpecializedScan(SCAN_NURSERY, G1NurseryTraceLocal.class);
    super.registerSpecializedMethods();
  }
}
