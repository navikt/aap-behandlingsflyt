package no.nav.aap.behandlingsflyt.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.avklaringsbehov.løsning.AvklarSamordningGraderingLøsning
import no.nav.aap.behandlingsflyt.steg.samordning.SamordningService
import no.nav.aap.misc.SamordningVurdering
import no.nav.aap.misc.SamordningVurderingGrunnlag
import no.nav.aap.misc.SamordningVurderingPeriode
import no.nav.aap.samordning.VurderingerForSamordning
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.steg.samordning.ytelsevurdering.SamordningVurderingRepository
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.LocalDateTime

class AvklarSamordningGraderingLøser(
    private val samordningYtelseVurderingRepository: SamordningVurderingRepository,
    private val samordningService: SamordningService,
) : AvklaringsbehovsLøser<AvklarSamordningGraderingLøsning> {

    constructor(repositoryProvider: RepositoryProvider) : this(
        samordningYtelseVurderingRepository = repositoryProvider.provide(),
        samordningService = SamordningService(repositoryProvider)
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

        val perioderSomIkkeHarBlittVurdert =
            samordningService.samordningGrunnlag(kontekst.behandlingId())
                .copy(vurderingGrunnlag = samordningsvurderinger)
                .perioderSomIkkeHarBlittVurdert()

        if (perioderSomIkkeHarBlittVurdert.isNotEmpty()) {
            throw UgyldigForespørselException(message = "Har ikke vurdert alle perioder for samordning med andre folketrygdytelser")
        }

        samordningYtelseVurderingRepository.lagreVurderinger(
            kontekst.kontekst.behandlingId, samordningsvurderinger
        )

        return LøsningsResultat("Vurdert samordning")
    }

    override fun forBehov(): Definisjon {
        return Definisjon.AVKLAR_SAMORDNING_GRADERING
    }
}