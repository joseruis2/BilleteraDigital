package com.banco.ms_pagos.dto.response;

public record ClienteInternoResponse(
        Long id,
        String nombres,
        String apellidos
) {
}