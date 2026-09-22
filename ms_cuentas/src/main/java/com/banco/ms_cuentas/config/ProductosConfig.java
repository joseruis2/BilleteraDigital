package com.banco.ms_cuentas.config;

public final class ProductosConfig {

    // No podemos hacer JOIN cross-DB contra ms_producto_db, asi que el id
    // del producto CUENTA_AHORRO queda fijo aqui. Segun tu script de seed,
    // 'PROD000001' (CUENTA_AHORRO) es el primer INSERT, por lo que su id
    // autoincremental deberia ser 1 en una base recien creada.
    // VERIFICA esto contra tu tabla producto_financiero real antes de usarlo en produccion:
    //   SELECT id FROM producto_financiero WHERE codigo_unico = 'PROD000001';
    public static final Long PRODUCTO_CUENTA_AHORRO_ID = 1L;

    private ProductosConfig() {
    }
}
