package no.nav.aap.behandlingsflyt.flyt.internals

import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.StrukturertDokument
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.NyÅrsakTilBehandlingV0
import no.nav.aap.verdityper.dokument.JournalpostId
import java.time.LocalDateTime

data class NyÅrsakTilBehandlingSakHendelse(
    val mottattTidspunkt: LocalDateTime,
    private val innsendingType: InnsendingType,
    val referanse: InnsendingReferanse,
    val strukturertDokument: StrukturertDokument<*>?
) : SakHendelse {
    override fun tilBehandlingHendelse(): BehandlingHendelse {
        return DokumentMottattBehandlingHendelse()
    }

    override fun getInnsendingType(): InnsendingType {
        return innsendingType
    }

    override fun getInnsendingReferanse(): InnsendingReferanse {
        return referanse
    }

    override fun getMelding(): StrukturertDokument<*>? {
        return strukturertDokument
    }
}
