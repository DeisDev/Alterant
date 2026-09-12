package com.deisdev.preserve.integration;

import com.deisdev.preserve.api.PreservationContext;
import com.deisdev.preserve.api.PreservationPermission;

/** Read-only authorization, repeated for every member before an authoritative commit. */
public interface OperationAccess {
    void validate(PreservationContext context, PreservationPermission.Change change);
    void validateItem();
}
