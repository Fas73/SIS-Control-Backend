package com.siscontrol.backend.config;

import org.hibernate.dialect.MariaDBDialect;

public class CustomMariaDBDialect extends MariaDBDialect {
    @Override
    public boolean supportsIfExistsAfterAlterTable() {
        return false;
    }
}
