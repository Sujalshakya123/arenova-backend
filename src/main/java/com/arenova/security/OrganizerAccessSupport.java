package com.arenova.security;

import com.arenova.dtos.enums.Role;
import com.arenova.dtos.enums.UserStatus;
import com.arenova.entities.User;
import com.arenova.exceptions.AccountBlockedException;

public final class OrganizerAccessSupport {

    private OrganizerAccessSupport() {
    }

    public static boolean isActiveOrganizer(User user) {
        return user != null
                && user.getRole() == Role.ORGANIZER
                && resolveStatus(user.getStatus()) == UserStatus.ACTIVE;
    }

    public static void requireActiveOrganizer(User user) {
        if (user == null) {
            throw new AccountBlockedException("Not authenticated.");
        }
        if (user.getRole() != Role.ORGANIZER) {
            return;
        }

        UserStatus status = resolveStatus(user.getStatus());
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
        if (status != UserStatus.ACTIVE) {
            throw new AccountBlockedException(AccountStatusSupport.blockedMessage(status));
        }
    }

    private static UserStatus resolveStatus(UserStatus status) {
        return status != null ? status : UserStatus.ACTIVE;
    }
}
