package no.nav.aap.behandlingsflyt.sakogbehandling.sak.flate

import no.nav.aap.behandlingsflyt.kontrakt.statistikk.ResultatKode
import no.nav.aap.komponenter.type.Periode
import java.time.LocalDateTime
import java.util.UUID

data class SaksinfoDTO(
    val saksnummer: String,
    val opprettetTidspunkt: LocalDateTime,
    val periode: Periode,
    val ident: String,
    val resultat: ResultatKode? = null,
    val sisteBehandlingReferanse: UUID? = null,
)

data class SaksInfoTilPostmottak(
    val saksnummer: String,
    val periode: Periode,
    val resultat: ResultatKode?,
    val finnesÅpenBehandling: Boolean,
    val harRettNåEllerIFramtiden: Boolean?
)