package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsopptrapping

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.LøsningForPeriode
import java.time.Instant
import java.time.LocalDate
import kotlin.String

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
            vurdertAv = avklaringsbehovKontekst.bruker.ident,
            opprettetTid = Instant.now(),
            vurderingenGjelderFra = fom,
            vurdertIBehandling = avklaringsbehovKontekst.behandlingId(),
            vurderingenGjelderTil = tom,
        )
}