package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarSamordningGraderingLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurderingGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurderingPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurderingRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.samordning.VurderingerForSamordning
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.LocalDateTime

class AvklarSamordningGraderingLøser(
    private val samordningYtelseVurderingRepository: SamordningVurderingRepository,
) : AvklaringsbehovsLøser<AvklarSamordningGraderingLøsning> {

    constructor(repositoryProvider: RepositoryProvider) : this(
        samordningYtelseVurderingRepository = repositoryProvider.provide(),
    )

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: AvklarSamordningGraderingLøsning): LøsningsResultat {
        val vurderingerForSamordning = løsning.vurderingerForSamordning
            .also(VurderingerForSamordning::valider)

        val samordningsvurderinger = SamordningVurderingGrunnlag(
            begrunnelse = vurderingerForSamordning.begrunnelse,
            vurderinger = vurderingerForSamordning.vurderteSamordningerData.groupBy { it.ytelseType }.map {
                SamordningVurdering(
                    ytelseType = it.key,
                    vurderingPerioder = it.value.map { vurdering ->
                        SamordningVurderingPeriode(
                            periode = vurdering.periode,
                            gradering = vurdering.gradering?.let(::Prosent),
                            manuell = vurdering.manuell
                        )
                    }.toSet()
                )
            }.toSet(),
            vurdertAv = kontekst.bruker,
            vurdertTidspunkt = LocalDateTime.now()
        )

        // Vi krever ikke lenger at saksbehandler har vurdert alle perioder som er funnet i register.
        // Saksbehandler kan bekrefte kortet uten å legge inn noen perioder i det hele tatt, jf. AAP-2277.
        samordningYtelseVurderingRepository.lagreVurderinger(
            kontekst.kontekst.behandlingId, samordningsvurderinger
        )

        return LøsningsResultat("Vurdert samordning")
    }

    override fun forBehov(): Definisjon {
        return Definisjon.AVKLAR_SAMORDNING_GRADERING
    }
}