package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarSamordningGraderingLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurderingGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurderingPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurderingRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.lookup.repository.RepositoryProvider

class AvklarSamordningGraderingLøser(connection: DBConnection) :
    AvklaringsbehovsLøser<AvklarSamordningGraderingLøsning> {
    private val samordningYtelseVurderingRepository =
        RepositoryProvider(connection).provide<SamordningVurderingRepository>()

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: AvklarSamordningGraderingLøsning): LøsningsResultat {

        val vurderingerForSamordning = løsning.vurderingerForSamordning
        samordningYtelseVurderingRepository.lagreVurderinger(
            kontekst.kontekst.behandlingId, SamordningVurderingGrunnlag(
                begrunnelse = vurderingerForSamordning.begrunnelse,
                maksDatoEndelig = vurderingerForSamordning.maksDatoEndelig,
                maksDato = vurderingerForSamordning.maksDato,
                vurderinger = vurderingerForSamordning.vurderteSamordningerData.groupBy { it.ytelseType }.map { SamordningVurdering(
                    ytelseType = it.key, vurderingPerioder = it.value.map { SamordningVurderingPeriode(
                        periode = it.periode,
                        gradering = it.gradering?.let(::Prosent),
                        kronesum = it.kronesum
                    ) },
                )}
            )
        )

        return LøsningsResultat("Vurdert samordning")
    }

    override fun forBehov(): Definisjon {
        return Definisjon.AVKLAR_SAMORDNING_GRADERING
    }
}