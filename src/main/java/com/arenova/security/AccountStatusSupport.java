package com.arenova.security;

import com.arenova.dtos.enums.Role;
import com.arenova.dtos.enums.UserStatus;
import com.arenova.entities.User;
import com.arenova.exceptions.AccountBlockedException;

public final class AccountStatusSupport {

    private AccountStatusSupport() {
    }

    public static boolean isUsable(UserStatus status) {
        return status == null || status == UserStatus.ACTIVE;
    }

    public static String blockedMessage(UserStatus status) {
        if (status == UserStatus.PENDING) {
            return "Your organizer account is still waiting for Super Admin approval.";
        }
        if (status == UserStatus.REJECTED) {
            return "Your organizer registration has been rejected. Please contact the administrator for more information.";
        }
        if (status == UserStatus.INACTIVE) {
            return "Your account is inactive. Contact support for help.";
        }
        return "Your account has been suspended. Contact support for help.";
    }

    public static void requireUsable(User user) {
        if (user == null) {
            return;
        }
        if (!isUsable(user.getStatus())) {
            throw new AccountBlockedException(blockedMessage(user.getStatus()));
        }
    }

    /** Login gate — organizer-specific pending/rejected messages before password check. */
    public static void requireLoginAllowed(User user) {
        if (user == null) {
            return;
        }
        if (user.getRole() == Role.ORGANIZER) {
            UserStatus status = user.getStatus() != null ? user.getStatus() : UserStatus.ACTIVE;
            if (status == UserStatus.PENDING || status == UserStatus.INACTIVE) {
                throw new AccountBlockedException(
                        "Your organizer account is still waiting for Super Admin approval."
                );
            }
            if (status == UserStatus.REJECTED) {
                throw new AccountBlockedException(
                        "Your organizer registration has been rejected. Please contact the administrator for more information."
                );
            }
        }
        requireUsable(user);
    }
}
