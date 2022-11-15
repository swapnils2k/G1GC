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
import org.mmtk.plan.generational.GenCollector;
import org.mmtk.plan.generational.GenMatureTraceLocal;
import org.mmtk.plan.Trace;
import org.mmtk.policy.Space;
import org.mmtk.utility.Log;
import org.mmtk.vm.VM;

import org.vmmagic.pragma.*;
import org.vmmagic.unboxed.*;

/**
 * This class implements the core functionality for a transitive
 * closure over the heap graph, specifically in a Generational copying
 * collector.
 */
// @Uninterruptible
public final class G1NurseryTraceLocal {
// public final class G1NurseryTraceLocal extends GenMatureTraceLocal {

  // /**
  //  * @param global the global trace class to use
  //  * @param plan the state of the generational collector
  //  */
  // public G1NurseryTraceLocal(Trace global, GenCollector plan) {
  //   super(global, plan);
  // }

  // private static G1 global() {
  //   return (G1) VM.activePlan.global();
  // }

  // /**
  //  * Trace a reference into the mature space during GC. This involves
  //  * determining whether the instance is in from space, and if so,
  //  * calling the <code>traceObject</code> method of the Copy
  //  * collector.
  //  *
  //  * @param object The object reference to be traced.  This is <i>NOT</i> an
  //  * interior pointer.
  //  * @return The possibly moved reference.
  //  */
  // @Override
  // public ObjectReference traceObject(ObjectReference object) {
  //   // Log.writeln("Inside G1 Mature Trace Local 1");
  //   if (VM.VERIFY_ASSERTIONS) VM.assertions._assert(global().traceFullHeap());
  //   if (object.isNull()) return object;
  //   // Log.writeln("Inside G1 Mature Trace Local 2");
  //   if (Space.isInSpace(G1.NS0, object))
  //     return G1.nursery0.traceObject(this, object, Gen.ALLOC_MATURE_MAJORGC);
  //   if (Space.isInSpace(G1.NS1, object))
  //     return G1.nursery1.traceObject(this, object, Gen.ALLOC_MATURE_MAJORGC);
  //     // Log.writeln("Inside G1 Mature Trace Local 3");
  //   return super.traceObject(object);
  // }

  // @Override
  // public boolean isLive(ObjectReference object) {
  //   if (object.isNull()) return false;
  //   if (Space.isInSpace(G1.NS0, object))
  //     return G1.hi ? G1.nursery0.isLive(object) : true;
  //   if (Space.isInSpace(G1.NS1, object))
  //     return G1.hi ? true : G1.nursery1.isLive(object);
  //   return super.isLive(object);
  // }

  // /****************************************************************************
  //  *
  //  * Object processing and tracing
  //  */


  // /**
  //  * {@inheritDoc}
  //  */
  // @Override
  // public boolean willNotMoveInCurrentCollection(ObjectReference object) {
  //   if (Space.isInSpace(G1.NS0, object)) {
  //     return true;
  //   }
  //   if (Space.isInSpace(G1.NS1, object)) {
  //     return false;
  //   }
  //   return super.willNotMoveInCurrentCollection(object);
  // }
}
