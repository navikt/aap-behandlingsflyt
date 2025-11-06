package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarSykdomLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.YrkesskadeGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.YrkesskadeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Sykdomsvurdering
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.komponenter.tidslinje.StandardSammenslåere
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.orEmpty
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory
import java.time.LocalDate

class AvklarSykdomLøser(
    private val behandlingRepository: BehandlingRepository,
    private val sykdomRepository: SykdomRepository,
    private val yrkersskadeRepository: YrkesskadeRepository,
    private val sakRepository: SakRepository
) : AvklaringsbehovsLøser<AvklarSykdomLøsning> {

    constructor(repositoryProvider: RepositoryProvider) : this(
        behandlingRepository = repositoryProvider.provide(),
        sykdomRepository = repositoryProvider.provide(),
        yrkersskadeRepository = repositoryProvider.provide(),
        sakRepository = repositoryProvider.provide()
    )

    private val log = LoggerFactory.getLogger(javaClass)

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: AvklarSykdomLøsning): LøsningsResultat {
        val behandling = behandlingRepository.hent(kontekst.kontekst.behandlingId)
        val yrkesskadeGrunnlag = yrkersskadeRepository.hentHvisEksisterer(behandling.id)

        val rettighetsperiode = sakRepository.hent(behandling.sakId).rettighetsperiode

        val nyeSykdomsvurderinger = løsning.sykdomsvurderinger
            .map { it.toSykdomsvurdering(kontekst.bruker, kontekst.behandlingId(), rettighetsperiode.fom) }
            .let {
                SykdomGrunnlag(
                    sykdomsvurderinger = it,
                    yrkesskadevurdering = null,
                ).somSykdomsvurderingstidslinje(LocalDate.MIN)
            }


        val eksisterendeSykdomsvurderinger = behandling.forrigeBehandlingId
            ?.let { sykdomRepository.hentHvisEksisterer(it) }
            ?.somSykdomsvurderingstidslinje(LocalDate.MIN)
            .orEmpty()

        val gjeldendeVurderinger = eksisterendeSykdomsvurderinger
            .kombiner(nyeSykdomsvurderinger, StandardSammenslåere.prioriterHøyreSideCrossJoin())
            .komprimer()
            .segmenter()
            .map { it.verdi }

        validerSykdomOgYrkesskadeKonsistens(nyeSykdomsvurderinger, yrkesskadeGrunnlag, behandling.typeBehandling())

        sykdomRepository.lagre(
            behandlingId = behandling.id,
            sykdomsvurderinger = gjeldendeVurderinger,
        )

        return LøsningsResultat(
            begrunnelse = "Vurdering av § 11-5"
        )
    }

    private fun validerSykdomOgYrkesskadeKonsistens(
        sykdomLøsning: Tidslinje<Sykdomsvurdering>,
        yrkesskadeGrunnlag: YrkesskadeGrunnlag?,
        typeBehandling: TypeBehandling
    ) {
        val harYrkesskade = yrkesskadeGrunnlag?.yrkesskader?.harYrkesskade() == true
        sykdomLøsning.segmenter().forEach {
            if (!it.verdi.erKonsistentForSykdom(harYrkesskade, typeBehandling)) {
                log.info(
                    "Sykdomsvurderingen er ikke konsistent med yrkesskade sykdomsvurdering=[{}] harYrkesskade=[{}]",
                    it.verdi,
                    harYrkesskade,
                )
                throw UgyldigForespørselException("Sykdomsvurdering og yrkesskade har ikke konsistente verdier")
            }
        }
    }

    override fun forBehov(): Definisjon {
        return Definisjon.AVKLAR_SYKDOM
    }
}
