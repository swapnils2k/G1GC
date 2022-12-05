package org.mmtk.plan.g1gc;

import org.vmmagic.pragma.*;

import org.mmtk.plan.StopTheWorldConstraints;
import org.mmtk.policy.CopySpace;
import org.mmtk.policy.Space;

@Uninterruptible
public class G1Constraints extends StopTheWorldConstraints {

  @Override
  public boolean generational() {
    return true;
  }
  
  @Override
  public boolean movesObjects() {
    return true;
  }

  @Override
  public int gcHeaderBits() {
    return CopySpace.LOCAL_GC_BITS_REQUIRED;
  }

  @Override
  public int gcHeaderWords() {
    return CopySpace.GC_HEADER_WORDS_REQUIRED;
  }

  @Override
  public int numSpecializedScans() {
    return 3;
  }

  @Override
  public int maxNonLOSDefaultAllocBytes() {
    /*
     * If the nursery is discontiguous, the maximum object is essentially unbounded.  In
     * a contiguous nursery, we can't attempt to nursery-allocate objects larger than the
     * available nursery virtual memory.
     */
    long fracAvailable = Space.getFracAvailable(G1.NURSERY_VM_FRACTION).toLong();
    if (fracAvailable > org.mmtk.utility.Constants.MAX_INT) {
      fracAvailable = org.mmtk.utility.Constants.MAX_INT;
    }
    return  (int)fracAvailable;  
  }

}
