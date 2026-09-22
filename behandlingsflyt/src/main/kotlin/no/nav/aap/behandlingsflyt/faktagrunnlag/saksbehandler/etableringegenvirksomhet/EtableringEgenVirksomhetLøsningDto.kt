package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.LøsningForPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Bruker
import java.time.Instant
import java.time.LocalDate
import kotlin.String

data class EtableringEgenVirksomhetLøsningDto(
    override val begrunnelse: String,
    override val fom: LocalDate,
    override val tom: LocalDate?,
    val virksomhetNavn: String,
    val orgNr: String? = null,
    val foreliggerFagligVurdering: Boolean,
    val virksomhetErNy: Boolean? = null,
    val brukerEierVirksomheten: EierVirksomhet? = null,
    val kanFøreTilSelvforsørget: Boolean? = null,

    val fase: EtableringFase? = null,
    val erRegistrertINødvendigeOffentligeRegister: Boolean? = null,

    //midlertid legacy, bare for overgang
    @Deprecated("Bruk fase + fom. Fjernes etter frontend mignering")
    val utviklingsPerioder: List<Periode>? = null,
    @Deprecated("Bruk fase + fom. Fjernes etter frontend mignering")
    val oppstartsPerioder: List<Periode>? = null
) : LøsningForPeriode {
    fun toEtableringEgenVirksomhetVurdering(avklaringsbehovKontekst: AvklaringsbehovKontekst) =
        toEtableringEgenVirksomhetVurdering(
            bruker = avklaringsbehovKontekst.bruker,
            vurdertIBehandling = avklaringsbehovKontekst.behandlingId(),
        )

    fun toEtableringEgenVirksomhetVurdering(bruker: Bruker, vurdertIBehandling: BehandlingId): EtableringEgenVirksomhetVurdering {
        val avklartFase = fase ?: when {
            oppstartsPerioder?.isNotEmpty() == true -> EtableringFase.OPPSTART
            utviklingsPerioder?.isNotEmpty() == true -> EtableringFase.UTVIKLING
            else -> null
        }

        // Legacy-innsending: gammel frontend kjenner ikke til dette feltet, men oppstart
        // ble alltid registrert i offentlige register i det gamle flytet.
        val avklartErRegistrert = erRegistrertINødvendigeOffentligeRegister
            ?: if (fase == null && avklartFase == EtableringFase.OPPSTART) true else erRegistrertINødvendigeOffentligeRegister

        val avklartFom = when (avklartFase){
            EtableringFase.UTVIKLING -> utviklingsPerioder?.firstOrNull()?.fom ?: this.fom
            EtableringFase.OPPSTART -> oppstartsPerioder?.firstOrNull()?.fom ?: this.fom
            null -> this.fom
        }

        val avklartTom = when (avklartFase){
            EtableringFase.UTVIKLING -> utviklingsPerioder?.firstOrNull()?.tom ?: this.tom
            EtableringFase.OPPSTART -> oppstartsPerioder?.firstOrNull()?.tom ?: this.tom
            null -> this.tom
        }

        return EtableringEgenVirksomhetVurdering(
            begrunnelse = begrunnelse,
            foreliggerFagligVurdering = foreliggerFagligVurdering,
            virksomhetErNy = virksomhetErNy,
            brukerEierVirksomheten = brukerEierVirksomheten,
            kanFøreTilSelvforsørget = kanFøreTilSelvforsørget,
            fase = avklartFase,
            erRegistrertINødvendigeOffentligeRegister = avklartErRegistrert,
            vurdertAv = bruker,
            opprettet = Instant.now(),
            vurdertIBehandling = vurdertIBehandling,
            fom = avklartFom,
            tom = avklartTom,
            virksomhetNavn = virksomhetNavn,
            orgNr = orgNr
        )
    }

}