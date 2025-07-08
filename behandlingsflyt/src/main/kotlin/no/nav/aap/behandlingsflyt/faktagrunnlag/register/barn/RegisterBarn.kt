package no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn

import no.nav.aap.behandlingsflyt.sakogbehandling.Ident

data class RegisterBarn(val id: Long, val identer: List<Ident>)