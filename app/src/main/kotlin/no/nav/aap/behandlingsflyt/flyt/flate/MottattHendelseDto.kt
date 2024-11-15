package no.nav.aap.behandlingsflyt.flyt.flate

import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.dokumenter.Brevkode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.dokumenter.Kanal

data class MottattHendelseDto (
    val saksnummer: String,
    val referanse: MottattDokumentReferanse,
    val type: Brevkode,
    val kanal: Kanal,
    val payload: Any?
)