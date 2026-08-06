package no.nav.aap.behandlingsflyt.behandling.søknad

import no.nav.aap.behandlingsflyt.behandling.søknad.flate.AarsakTilTrekkSoknadDto
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.verdityper.dokument.JournalpostId
import java.time.Instant

data class TrukketSøknadVurdering(
    val journalpostId: JournalpostId,
    val begrunnelse: String,
    val skalTrekkes: Boolean,
    val vurdertAv: Bruker,
    val vurdert: Instant,
    val aarsak: AarsakTilTrekkSoknad?,
)

enum class AarsakTilTrekkSoknad {
    FOR_TIDLIG,
    FEIL_YTELSE,
    BRUKER_ONSKER_IKKE,
    ANNET,
}

fun AarsakTilTrekkSoknad.tilDto(): AarsakTilTrekkSoknadDto =
    when (this) {
        AarsakTilTrekkSoknad.FOR_TIDLIG -> AarsakTilTrekkSoknadDto.FOR_TIDLIG
        AarsakTilTrekkSoknad.FEIL_YTELSE -> AarsakTilTrekkSoknadDto.FEIL_YTELSE
        AarsakTilTrekkSoknad.BRUKER_ONSKER_IKKE -> AarsakTilTrekkSoknadDto.BRUKER_ONSKER_IKKE
        AarsakTilTrekkSoknad.ANNET -> AarsakTilTrekkSoknadDto.ANNET
    }


