package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarPeriodisertLovvalgMedlemskapLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.PeriodisertAvklaringsbehovLøsning
import no.nav.aap.behandlingsflyt.behandling.lovvalg.tilTidslinje
import no.nav.aap.behandlingsflyt.behandling.lovvalg.validerGyldigForRettighetsperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.ManuellVurderingForLovvalgMedlemskap
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.PeriodisertManuellVurderingForLovvalgMedlemskapDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapArbeidInntektRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.komponenter.tidslinje.StandardSammenslåere
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.LocalDateTime

class AvklarPeriodisertLovvalgMedlemskapLøser(
    private val medlemskapArbeidInntektRepository: MedlemskapArbeidInntektRepository,
    private val behandlingRepository: BehandlingRepository,
    private val sakRepository: SakRepository,
) : AvklaringsbehovsLøser<AvklarPeriodisertLovvalgMedlemskapLøsning> {

    constructor(repositoryProvider: RepositoryProvider) : this(
        medlemskapArbeidInntektRepository = repositoryProvider.provide(),
        behandlingRepository = repositoryProvider.provide(),
        sakRepository = repositoryProvider.provide(),
    )

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: AvklarPeriodisertLovvalgMedlemskapLøsning): LøsningsResultat {
        val behandling = behandlingRepository.hent(kontekst.kontekst.behandlingId)
        val sak = sakRepository.hent(behandling.sakId)
        val nyeVurderinger = løsning.løsningerForPerioder.map {
            ManuellVurderingForLovvalgMedlemskap(
                fom = it.fom,
                tom = it.tom,
                vurdertIBehandling = behandling.id,
                lovvalgVedSøknadsTidspunkt = it.lovvalg,
                medlemskapVedSøknadsTidspunkt = it.medlemskap,
                vurdertAv = kontekst.bruker.ident,
                vurdertDato = LocalDateTime.now(),
                overstyrt = true
            )
        }

        val tidligereVurderinger =
            kontekst.kontekst.forrigeBehandlingId?.let { medlemskapArbeidInntektRepository.hentHvisEksisterer(it) }?.vurderinger
                ?: emptyList()
        val komplettTidslinje = tidligereVurderinger.tilTidslinje()
            .kombiner(nyeVurderinger.tilTidslinje(), StandardSammenslåere.prioriterHøyreSideCrossJoin())
        val vurderinger = tidligereVurderinger + nyeVurderinger

        // Krever manuelle vurderinger for hele perioden
        komplettTidslinje.validerGyldigForRettighetsperiode(sak.rettighetsperiode)
            .throwOnInvalid { UgyldigForespørselException("Løsningen for vurdert lovvalg og medlemskap er ikke gyldig: ${it.errorMessage}") }
            .onValid {
                medlemskapArbeidInntektRepository.lagreVurderinger(
                    behandlingId = behandling.id,
                    vurderinger = vurderinger
                )
            }

        return LøsningsResultat("Vurdert lovvalg & medlemskap manuelt.")
    }

    override fun forBehov(): Definisjon {
        return Definisjon.AVKLAR_LOVVALG_MEDLEMSKAP
    }
}