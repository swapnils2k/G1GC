package org.mmtk.plan.g1gc;

import org.mmtk.plan.PlanConstraints;
import org.vmmagic.pragma.*;

@Uninterruptible
public class G1Constraints extends PlanConstraints {
    
  @Override
  public int gcHeaderBits() {
    return 0;
  }
  @Override
  public int gcHeaderWords() {
    return 0;
  }
}
