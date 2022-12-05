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
  public boolean needsObjectReferenceWriteBarrier() {
    return true;
  }

  @Override
  public boolean objectReferenceBulkCopySupported() {
    return true;
  }

  @Override
  public int numSpecializedScans() {
    return 3;
  }
}
