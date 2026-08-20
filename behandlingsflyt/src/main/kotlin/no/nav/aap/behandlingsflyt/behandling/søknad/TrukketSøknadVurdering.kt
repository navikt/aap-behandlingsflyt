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
    BRUKER_SOKTE_FOR_TIDLIG,
    BRUKER_SOKTE_FEIL_YTELSE,
    BRUKER_ONSKER_IKKE_SOKE_LENGER,
    ANNET,
}

fun AarsakTilTrekkSoknad?.tilDto(): AarsakTilTrekkSoknadDto? =
    when (this) {
        AarsakTilTrekkSoknad.BRUKER_SOKTE_FOR_TIDLIG -> AarsakTilTrekkSoknadDto.BRUKER_SOKTE_FOR_TIDLIG
        AarsakTilTrekkSoknad.BRUKER_SOKTE_FEIL_YTELSE -> AarsakTilTrekkSoknadDto.BRUKER_SOKTE_FEIL_YTELSE
        AarsakTilTrekkSoknad.BRUKER_ONSKER_IKKE_SOKE_LENGER -> AarsakTilTrekkSoknadDto.BRUKER_ONSKER_IKKE_SOKE_LENGER
        AarsakTilTrekkSoknad.ANNET -> AarsakTilTrekkSoknadDto.ANNET
        null -> null
    }


