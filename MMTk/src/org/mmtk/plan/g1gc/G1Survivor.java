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
public class G1Survivor extends G1Nursery {

  public static final int ALLOC_SURVIVOR    = StopTheWorld.ALLOCATORS + 1;
  public static final int SCAN_SURVIVOR     = 1;


  /* Space object definition */
  public static final CopySpace survivorSpace = new CopySpace("survivor", false, VMRequest.discontiguous());
  public static final int SURVIVOR = survivorSpace.getDescriptor();
  public static final Address SURVIVOR_START = survivorSpace.getStart();

  public final Trace survivorTrace = new Trace(metaDataSpace);

  public final SharedDeque s_modbufPool = new SharedDeque("modBufs",metaDataSpace, 1);
  public final SharedDeque s_remsetPool = new SharedDeque("remSets",metaDataSpace, 1);
  public final SharedDeque s_arrayRemsetPool = new SharedDeque("arrayRemSets",metaDataSpace, 2);

  @Override
  @NoInline
  public void collectionPhase(short phaseId) {
    if (phaseId == PREPARE) {
      if(isCurrentGCSurvivor()) {
        survivorSpace.prepare(true);
        return;
      }

      if(traceFullHeap()) {
        survivorSpace.prepare(true);
        super.collectionPhase(phaseId);
        s_remsetPool.clearDeque(1);
        s_arrayRemsetPool.clearDeque(2);
        return;
      }
    }

    if (phaseId == STACK_ROOTS) {
      VM.scanning.notifyInitialThreadScanComplete(!traceFullHeap());
      setGCStatus(GC_PROPER);
      return;
    }

    if (phaseId == CLOSURE) {
      if(isCurrentGCSurvivor()) {
        survivorTrace.prepare();
        return;
      }
    }

    if (phaseId == RELEASE) {
      if(isCurrentGCSurvivor() || traceFullHeap()) {
          survivorSpace.release();
          s_modbufPool.clearDeque(1);
          s_remsetPool.clearDeque(1);
          s_arrayRemsetPool.clearDeque(2);
          if(isCurrentGCSurvivor())
            survivorTrace.release();

          nextGCSurvivor = false;
          gcSurvivor = false;
      }
      super.collectionPhase(phaseId);
      return;
    }

    super.collectionPhase(phaseId);
  }

  @Override
  public boolean collectionRequired(boolean spaceFull, Space space) {
      if(space == survivorSpace && spaceFull) {
          nextGCSurvivor = true;
          return true;
      }

      return super.collectionRequired(spaceFull, space);
  }

  @Override
  public int getPagesUsed() {
    return (survivorSpace.reservedPages() + super.getPagesUsed());
  }

  @Override
  public int getCollectionReserve() {
    return survivorSpace.reservedPages() + super.getCollectionReserve();
  }
  
  @Inline
  static boolean inSurvivor(Address addr) {
      return addr.GE(SURVIVOR_START);
  }

  @Inline
  static boolean inSurvivor(ObjectReference obj) {
    return inSurvivor(obj.toAddress());
  }

  @Override
  public boolean willNeverMove(ObjectReference object) {
    if (Space.isInSpace(SURVIVOR, object))
      return false;

    return super.willNeverMove(object);
  }

  @Override
  @Interruptible
  public void registerSpecializedMethods() {
    TransitiveClosure.registerSpecializedScan(SCAN_SURVIVOR, GenNurseryTraceLocal.class);
    super.registerSpecializedMethods();
  }

}
