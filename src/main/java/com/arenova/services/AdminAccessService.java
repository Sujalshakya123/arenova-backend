package com.arenova.services;

import com.arenova.entities.User;

public interface AdminAccessService {

    User requireAdmin();
}
