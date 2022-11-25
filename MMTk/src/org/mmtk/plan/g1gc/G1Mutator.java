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


import org.mmtk.plan.generational.Gen;
import org.mmtk.plan.generational.GenMutator;
import org.mmtk.policy.CopyLocal;
import org.mmtk.policy.CopySpace;
import org.mmtk.policy.Space;
import org.mmtk.utility.Log;
import org.mmtk.utility.alloc.Allocator;
import org.mmtk.vm.VM;

import org.vmmagic.unboxed.*;
import org.vmmagic.pragma.*;

/**
 * This class implements <i>per-mutator thread</i> behavior and state for
 * the <code>GenCopy</code> two-generational copying collector.<p>
 *
 * Specifically, this class defines mutator-time semantics specific to the
 * mature generation (<code>GenMutator</code> defines nursery semantics).
 * In particular the mature space allocator is defined (for mutator-time
 * allocation into the mature space via pre-tenuring), and the mature space
 * per-mutator thread collection time semantics are defined (rebinding
 * the mature space allocator).<p>
 *
 * @see GenCopy for a description of the <code>GenCopy</code> algorithm.
 *
 * @see GenCopy
 * @see GenCopyCollector
 * @see GenMutator
 * @see org.mmtk.plan.StopTheWorldMutator
 * @see org.mmtk.plan.MutatorContext
 */
@Uninterruptible
public class G1Mutator extends GenMutator {
  /******************************************************************
   * Instance fields
   */

  /**
   * The allocator for the copying mature space (the mutator may
   * "pretenure" objects into this space otherwise used only by
   * the collector)
   */
  private final CopyLocal mature0;
  private final CopyLocal mature1;
  private boolean flag = false;

  /****************************************************************************
   *
   * Initialization
   */

  /**
   * Constructor
   */
  public G1Mutator() {
    mature0 = new CopyLocal(G1Mature.matureSpace0);
    mature1 = new CopyLocal(G1Mature.matureSpace1);
  }

  /**
   * Called before the MutatorContext is used, but after the context has been
   * fully registered and is visible to collection.
   */
  @Override
  public void initMutator(int id) {
    super.initMutator(id);
    // mature.rebind(G1.toSpace());
  }

  /****************************************************************************
   *
   * Mutator-time allocation
   */

  /**
   * {@inheritDoc}
   */
  @Override
  @Inline
  public final Address alloc(int bytes, int align, int offset, int allocator, int site) {
    // Log.writeln("G1 Allocation called");
    Address returnVal = null;
    // Log.write("Id = ");
    // Log.writeln(this.getId());
    // Log.write("Allocator = ");
    // Log.write(allocator);
    if (allocator == Gen.ALLOC_NURSERY) {
      // Log.write("G1 Mature Allocator");
      if (flag) {
        flag = !flag;
        Log.writeln("Allocating in mature0 ");
        returnVal = mature0.alloc(bytes, align, offset);
      } else {
        flag = !flag;
        Log.writeln("Allocating in mature1 ");
        returnVal = mature1.alloc(bytes, align, offset);
      }
    }
    if (returnVal != null) {
      ObjectReference ref = returnVal.toObjectReference();
      if (Space.isInSpace(G1Mature.MS0, ref)) {
        Log.writeln("In nursery");
        Space.printVMMap();
        Log.writeln(returnVal);
      }
      return returnVal;
    }
    // Log.write("Super Calling");
    returnVal = super.alloc(bytes, align, offset, allocator, site);
    return returnVal;
  }

  @Override
  @Inline
  public final void postAlloc(ObjectReference object, ObjectReference typeRef,
      int bytes, int allocator) {
    // nothing to be done
    if (allocator == G1Mature.ALLOC_MATURE) return;
    super.postAlloc(object, typeRef, bytes, allocator);
  }

  // @Override
  // public final Allocator getAllocatorFromSpace(Space space) {
  //   if (space == G1.matureSpace0 || space == G1.matureSpace1) return mature;
  //   return super.getAllocatorFromSpace(space);
  // }


  /*****************************************************************************
   *
   * Collection
   */

  /**
   * {@inheritDoc}
   */
  @Override
  public void collectionPhase(short phaseId, boolean primary) {
    Log.writeln("GC Collection triggered.");
    if (global().traceFullHeap()) {
      if (phaseId == G1Mature.RELEASE) {
        super.collectionPhase(phaseId, primary);
        // if (global().gcFullHeap) mature.rebind(G1.toSpace());
        return;
      }
    }

    super.collectionPhase(phaseId, primary);
  }

  /*****************************************************************************
   *
   * Miscellaneous
   */

  /** @return The active global plan as a <code>G1</code> instance. */
  private static G1Mature global() {
    return (G1Mature) VM.activePlan.global();
  }

}
