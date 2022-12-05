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

import org.mmtk.*;
import org.mmtk.policy.CopySpace;
import org.mmtk.policy.Space;
import org.mmtk.plan.generational.*;
import org.mmtk.plan.Trace;
import org.mmtk.plan.TransitiveClosure;
import org.mmtk.utility.heap.VMRequest;
import org.mmtk.vm.VM;

import org.vmmagic.unboxed.Address;
import org.vmmagic.unboxed.ObjectReference;

import org.mmtk.plan.*;
import org.mmtk.utility.deque.*;
import org.mmtk.utility.heap.layout.HeapLayout;
import org.mmtk.utility.Log;
import org.mmtk.utility.options.Options;
import org.mmtk.utility.sanitychecker.SanityChecker;
import org.mmtk.utility.statistics.*;


import org.vmmagic.pragma.*;

@Uninterruptible 
public class G1 extends G1Survivor {

  public static final int ALLOC_MATURE         = StopTheWorld.ALLOCATORS + 2;
  public static final int SCAN_MATURE          = 2;

  static boolean hi = false;

  public static final CopySpace matureSpace0 = new CopySpace("matureSpace0", false, VMRequest.discontiguous());
  public static final int MS0 = matureSpace0.getDescriptor();
  public static final Address MATURE0_START = matureSpace0.getStart();

  public static final CopySpace matureSpace1 = new CopySpace("matureSpace1", true, VMRequest.discontiguous());
  static final int MS1 = matureSpace1.getDescriptor();
  public static final Address MATURE1_START = matureSpace1.getStart();

  final Trace matureTrace = new Trace(metaDataSpace);


  static CopySpace toSpace() {
    return hi ? matureSpace1 : matureSpace0;
  }

  static int toSpaceDesc() {
    return hi ? MS1 : MS0;
  }

  static CopySpace fromSpace() {
    return hi ? matureSpace0 : matureSpace1;
  }

  static int fromSpaceDesc() {
    return hi ? MS0 : MS1;
  }

  @Override
  @Inline
  public void collectionPhase(short phaseId) {
    Log.write("\nCollection is invoked for G1GC");
    if(phaseId == PREPARE) {
        Log.write("\nnextGCNursery = ");
        Log.write(nextGCNursery);
        Log.write("\nnextGCSurvivor = ");
        Log.write(nextGCSurvivor);
        Log.write("\nnextGCFullHeap = ");
        Log.write(nextGCFullHeap);
        Log.write("\ngcNursery = ");
        Log.write(gcNursery);
        Log.write("\ngcSurvivor = ");
        Log.write(gcSurvivor);
        Log.write("\ngcFullHeap = ");
        Log.write(gcFullHeap);
    }

    if (traceFullHeap()) {
      if (phaseId == PREPARE) {
        super.collectionPhase(phaseId);
        hi = !hi;
        matureSpace0.prepare(hi);
        matureSpace1.prepare(!hi);
        matureTrace.prepare();
        return;
      }

      if (phaseId == CLOSURE) {
        matureTrace.prepare();
        return;
      }

      if (phaseId == RELEASE) {
        matureTrace.release();
        fromSpace().release();
        super.collectionPhase(phaseId);
        return;
      }
    }
    super.collectionPhase(phaseId);
  }


  @Override
  public boolean collectionRequired(boolean spaceFull, Space space) {
      if(space == toSpace() && spaceFull) {
          nextGCFullHeap = true;
          return true;
      }

      return super.collectionRequired(spaceFull, space);
  }

  @Inline
  static boolean inMature(Address addr) {
      return addr.GE(MATURE0_START) || addr.GE(MATURE1_START);
  }

  @Inline
  static boolean inMature(ObjectReference obj) {
    return inSurvivor(obj.toAddress());
  }

  @Override
  @Inline
  public int getPagesUsed() {
    return toSpace().reservedPages() + super.getPagesUsed();
  }

  @Override
  public final int getCollectionReserve() {
    return toSpace().reservedPages() + super.getCollectionReserve();
  }

  @Override
  @Interruptible
  public void registerSpecializedMethods() {
    TransitiveClosure.registerSpecializedScan(SCAN_MATURE, G1MatureTraceLocal.class);
    super.registerSpecializedMethods();
  }
}
