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
package org.mmtk.plan.generational;

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

/**
 * This abstract class implements the core functionality of generic
 * two-generation copying collectors.  Nursery collections occur when
 * either the heap is full or the nursery is full.  The nursery size
 * is determined by an optional command line argument.  If undefined,
 * the nursery size is "infinite", so nursery collections only occur
 * when the heap is full (this is known as a flexible-sized nursery
 * collector).  Thus both fixed and flexible nursery sizes are
 * supported.  Full heap collections occur when the nursery size has
 * dropped to a statically defined threshold,
 * <code>NURSERY_THRESHOLD</code><p>
 *
 * See also Plan.java for general comments on local vs global plan
 * classes.
 */
@Uninterruptible
public abstract class G1Survivor extends G1Nursery {

  /* The nursery space is where all new objects are allocated by default */
  public static final CopySpace survivorSpace = new CopySpace("survivor", false, vmRequest);
  public static final int SURVIVOR = survivorSpace.getDescriptor();
  private static final Address SURVIVOR_START = survivorSpace.getStart();

  /*****************************************************************************
   *
   * Instance fields
   */


  /* The trace object */
  public final Trace survivorTrace = new Trace(metaDataSpace);

  /**
   * Remset pools
   */

  /**
   *
   */
  public final SharedDeque s_modbufPool = new SharedDeque("s_modBufs",metaDataSpace, 1);
  public final SharedDeque s_remsetPool = new SharedDeque("s_remSets",metaDataSpace, 1);
  public final SharedDeque s_arrayRemsetPool = new SharedDeque("s_arrayRemSets",metaDataSpace, 2);


  /*****************************************************************************
   *
   * Collection
   */

  @Override
  @NoInline
  public void collectionPhase(short phaseId) {
    if (phaseId == PREPARE) {
      survivorSpace.prepare(true);
      return super.collectionPhase(phaseId);
    }

    if (phaseId == CLOSURE) {
      if (gcSurvivor) {
        survivorTrace.prepare();
      }
      return;
    }

    if (phaseId == RELEASE) {
      survivorSpace.release();
      switchNurseryZeroingApproach(survivorSpace);
      s_modbufPool.clearDeque(1);
      s_remsetPool.clearDeque(1);
      s_arrayRemsetPool.clearDeque(2);

      if (gcSurvivor) {
        survivorTrace.release();
      } else {
        super.collectionPhase(phaseId);
      }
     
      gcSurvivor = false;
      nextGCSurvivor = false;
      return;
    }

    super.collectionPhase(phaseId);
  }

  @Override
  // Need to write in survivor too 
  public final boolean collectionRequired(boolean spaceFull, Space space) {
    int availableSurvivorPages = Options.nurserySize.getMaxNursery() - survivorSpace.reservedPages();
    if (availableSurvivorPages <= 0) {
      nextGCSurvivor = true;
      return true;
    }

    if (spaceFull && space == survivorSpace) {
      nextGCSurvivor = true;
      return true;
    }

    return super.collectionRequired(spaceFull, space);
  }

  /*****************************************************************************
   *
   * Correctness
   */

  /*****************************************************************************
   *
   * Accounting
   */

  /**
   * {@inheritDoc}
   * Simply add the nursery's contribution to that of
   * the superclass.
   */
  @Override
  public int getPagesUsed() {
    return (survivorSpace.reservedPages() + super.getPagesUsed());
  }

  /**
   * Return the number of pages available for allocation, <i>assuming
   * all future allocation is to the nursery</i>.
   *
   * @return The number of pages available for allocation, <i>assuming
   * all future allocation is to the nursery</i>.
   */
  @Override
  public int getPagesAvail() {
    return super.getPagesAvail() >> 1;
  }

  /**
   * Return the number of pages reserved for collection.
   */
  @Override
  public int getCollectionReserve() {
    return survivorSpace.reservedPages() + super.getCollectionReserve();
  }


  @Override
  @Interruptible
  protected void registerSpecializedMethods() {
    TransitiveClosure.registerSpecializedScan(SCAN_SURVIVOR, GenNurseryTraceLocal.class);
    super.registerSpecializedMethods();
  }

  @Interruptible
  @Override
  public void fullyBooted() {
    super.fullyBooted();
    survivorSpace.setZeroingApproach(true, false);
  }
}
