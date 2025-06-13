package no.nav.aap.behandlingsflyt.flyt.internals

import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.StrukturertDokument
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType

interface SakHendelse {
    fun getMelding(): StrukturertDokument<*>?
    fun tilBehandlingHendelse(): BehandlingHendelse
    fun getInnsendingType(): InnsendingType
    fun getInnsendingReferanse(): InnsendingReferanse
}
