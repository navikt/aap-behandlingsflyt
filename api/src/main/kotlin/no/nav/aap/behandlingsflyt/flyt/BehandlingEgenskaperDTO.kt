package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.stønadsperiode.KantIKantMedArena
import java.time.LocalDate

data class BehandlingEgenskaperDTO(
    val erMigrertFraArena: Boolean,
    val harArenaHistorikk: Boolean,
    val erKantIKantMedArena: Boolean,
    val kantIKantMedArena: KantIKantMedArenaDTO?,
)

data class KantIKantMedArenaDTO(
    val arenaSaksnummer: String,
    val status: String,
    val maksdato: LocalDate,
    val vedtakTil: LocalDate?,
    val vedtakId: Int,
)

fun KantIKantMedArena.tilDTO() = KantIKantMedArenaDTO(
    arenaSaksnummer = arenaSaksnummer,
    status = status,
    maksdato = maksdato,
    vedtakTil = vedtakTil,
    vedtakId = vedtakId,
)
