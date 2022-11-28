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

  /* Space object definition */
  public static final CopySpace nurserySpace = new CopySpace("nursery", false, VMRequest.discontiguous());
  public static final int NURSERY = nurserySpace.getDescriptor();
  public static final Address NURSERY_START = nurserySpace.getStart();
  
  /* The trace object */
  public final Trace nurseryTrace = new Trace(metaDataSpace);

  /* Remember set */
  public final SharedDeque modbufPool = new SharedDeque("modBufs",metaDataSpace, 1);
  public final SharedDeque remsetPool = new SharedDeque("remSets",metaDataSpace, 1);
  public final SharedDeque arrayRemsetPool = new SharedDeque("arrayRemSets",metaDataSpace, 2);

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
        nurserySpace.prepare(true);
        super.collectionPhase(phaseId);
        remsetPool.clearDeque(1);
        arrayRemsetPool.clearDeque(2);
        return;
      }
    }

    if (phaseId == STACK_ROOTS) {
      VM.scanning.notifyInitialThreadScanComplete(!traceFullHeap());
      setGCStatus(GC_PROPER);
      return;
    }

    if (phaseId == CLOSURE) {
      if(isCurrentGCNursery()) {
        nurseryTrace.prepare();
        return;
      }
    }

    if (phaseId == RELEASE) {
      if(isCurrentGCNursery() || traceFullHeap()) {
          nurserySpace.release();
          modbufPool.clearDeque(1);
          remsetPool.clearDeque(1);
          arrayRemsetPool.clearDeque(2);
          if(isCurrentGCNursery())
            nurseryTrace.release();

          nextGCNursery = false;
          gcNursery = false;
      }
      super.collectionPhase(phaseId);
      return;
    }

    super.collectionPhase(phaseId);
  }

  @Override
  public boolean collectionRequired(boolean spaceFull, Space space) {
      if(space == nurserySpace && spaceFull) {
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
  
  @Inline
  static boolean inNursery(Address addr) {
      return addr.GE(NURSERY_START);
  }

  @Inline
  static boolean inNursery(ObjectReference obj) {
    return inNursery(obj.toAddress());
  }

  public final boolean traceFullHeap() {
    return gcFullHeap;
  }

  @Override
  public final boolean isCurrentGCNursery() {
    return gcNursery;
  }

  public final boolean isCurrentGCSurvivor() {
    return gcSurvivor;
  }

  @Override
  public boolean willNeverMove(ObjectReference object) {
    if (Space.isInSpace(NURSERY, object))
      return false;

    return super.willNeverMove(object);
  }

  @Override
  @Interruptible
  public void registerSpecializedMethods() {
    TransitiveClosure.registerSpecializedScan(SCAN_NURSERY, GenNurseryTraceLocal.class);
    super.registerSpecializedMethods();
  }
}
