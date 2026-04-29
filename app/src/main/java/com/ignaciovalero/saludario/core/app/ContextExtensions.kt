package com.ignaciovalero.saludario.core.app

import android.content.Context
import com.ignaciovalero.saludario.SaludarioApplication

/**
 * Devuelve la instancia de [SaludarioApplication] asociada a este [Context]
 * o `null` si por algún motivo el contexto no pertenece a la app (caso
 * extremadamente raro: builds con `tools:replace="application/name"` o tests
 * con un Application distinto).
 *
 * Usar esta extensión en lugar de `applicationContext as SaludarioApplication`
 * evita NPE silenciosos y centraliza el cast en un único punto.
 */
val Context.saludarioApp: SaludarioApplication?
    get() = applicationContext as? SaludarioApplication
