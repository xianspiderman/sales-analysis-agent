package com.dyh.salesAgent.memory;

import com.dyh.salesAgent.security.UserContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserScopedMemoryIdTest {

    @Test
    void sameClientSessionShouldBeIsolatedByUser() {
        UserContext.UserInfo manager = new UserContext.UserInfo(2L, "manager", "SALES_MANAGER", 1L, 2L);
        UserContext.UserInfo salesRep = new UserContext.UserInfo(3L, "salesRep", "SALES_REP", 1L, 3L);

        String managerMemoryId = UserScopedMemoryId.from(manager, "apipost-sync-001");
        String repMemoryId = UserScopedMemoryId.from(salesRep, "apipost-sync-001");

        assertEquals("2:apipost-sync-001", managerMemoryId);
        assertEquals("3:apipost-sync-001", repMemoryId);
        assertNotEquals(managerMemoryId, repMemoryId);
    }

    @Test
    void missingUserShouldFailClosed() {
        assertThrows(IllegalStateException.class,
                () -> UserScopedMemoryId.from(null, "apipost-sync-001"));
    }
}
