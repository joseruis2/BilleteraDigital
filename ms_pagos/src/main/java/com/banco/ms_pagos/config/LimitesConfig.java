package com.banco.ms_pagos.config;

import java.math.BigDecimal;

public final class LimitesConfig {

    // Limite diario por defecto al abrir cuenta. Si mas adelante esto varia
    // por tipo de producto o segmento de cliente, se mueve a configuracion externa.
    public static final BigDecimal LIMITE_DIARIO_DEFAULT = new BigDecimal("2000.00");

    private LimitesConfig() {
    }
}