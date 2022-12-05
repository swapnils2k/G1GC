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
  public static final CopySpace survivorSpace = new CopySpace("survivor", false, vmRequest);
  public static final int SURVIVOR = survivorSpace.getDescriptor();
  public static final Address SURVIVOR_START = survivorSpace.getStart();

  public final Trace survivorTrace = new Trace(metaDataSpace);

  @Override
  @NoInline
  public void collectionPhase(short phaseId) {
    if (phaseId == PREPARE) {
      if(isCurrentGCSurvivor()) {
        survivorSpace.prepare(true);
        return;
      }

      if(traceFullHeap()) {
        super.collectionPhase(phaseId);
        survivorSpace.prepare(true);
        return;
      }
    }

    if (phaseId == CLOSURE) {
      if(isCurrentGCSurvivor()) {
        survivorTrace.prepare();
        return;
      }

      if(traceFullHeap()) {
        super.collectionPhase(phaseId);
        survivorTrace.prepare();
        return; 
      }
    }

    if (phaseId == RELEASE) {
      if(isCurrentGCSurvivor()) {
          survivorSpace.release();
          survivorTrace.release();
      }

      if(traceFullHeap()) {
          survivorSpace.release();
          survivorTrace.release();
          super.collectionPhase(phaseId);
      }

      nextGCSurvivor = false;
      gcSurvivor = false;
      return;
    }

    super.collectionPhase(phaseId);
  }

  @Override
  public boolean collectionRequired(boolean spaceFull, Space space) {
      if(space == survivorSpace && spaceFull) {
          Log.write("Since space object is equal to survivor space, setting nextGCSurvivor as true");
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
  @Interruptible
  public void registerSpecializedMethods() {
    TransitiveClosure.registerSpecializedScan(SCAN_SURVIVOR, G1SurvivorTraceLocal.class);
    super.registerSpecializedMethods();
  }

}

