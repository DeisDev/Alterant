package com.deisdev.alterant.integration;

import com.deisdev.alterant.api.PreservationContext;
import com.deisdev.alterant.api.PreservationPermission;

/** Read-only authorization, repeated for every member before an authoritative commit. */
public interface OperationAccess {
    void validate(PreservationContext context, PreservationPermission.Change change);
    void validateItem();
}
