package no.nav.aap.behandlingsflyt.steg.arbeidsopptrapping

import no.nav.aap.behandlingsflyt.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.avklaringsbehov.løsning.LøsningForPeriode
import java.time.Instant
import java.time.LocalDate
import kotlin.String
import no.nav.aap.arbeidsopptrapping.ArbeidsopptrappingVurdering

data class ArbeidsopptrappingLøsningDto(
    override val begrunnelse: String,
    val reellMulighetTilOpptrapping: Boolean,
    val rettPaaAAPIOpptrapping: Boolean,
    override val fom: LocalDate,
    override val tom: LocalDate?,
) : LøsningForPeriode {
    fun toArbeidsopptrappingVurdering(avklaringsbehovKontekst: AvklaringsbehovKontekst) =
        ArbeidsopptrappingVurdering(
            begrunnelse = begrunnelse,
            reellMulighetTilOpptrapping = reellMulighetTilOpptrapping,
            rettPaaAAPIOpptrapping = rettPaaAAPIOpptrapping,
            vurdertAv = avklaringsbehovKontekst.bruker,
            opprettet = Instant.now(),
            fom = fom,
            vurdertIBehandling = avklaringsbehovKontekst.behandlingId(),
            tom = tom,
        )
}