package no.nav.aap.behandlingsflyt.flyt.internals

import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.StrukturertDokument
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.NyÅrsakTilBehandlingV0
import no.nav.aap.komponenter.type.Periode
import java.time.LocalDateTime

class NyÅrsakTilBehandlingHendelse(
    val mottattTidspunkt: LocalDateTime,
    val innsendingType: InnsendingType,
    val referanse: InnsendingReferanse,
    val strukturertDokument: StrukturertDokument<NyÅrsakTilBehandlingV0>?,
    val periode: Periode
) : PersonHendelse {
    override fun periode(): Periode {
        return periode
    }

    override fun tilSakshendelse(): SakHendelse {
        return NyÅrsakTilBehandlingSakHendelse(mottattTidspunkt, innsendingType, referanse, strukturertDokument)
    }
}
