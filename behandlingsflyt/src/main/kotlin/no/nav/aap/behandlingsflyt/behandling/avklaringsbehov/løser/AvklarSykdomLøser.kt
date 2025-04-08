package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarSykdomLøsning
import no.nav.aap.behandlingsflyt.exception.UgyldigForespørselException
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.YrkesskadeGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.YrkesskadeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Sykdomsvurdering
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.tidslinje.StandardSammenslåere
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory
import java.time.LocalDate

class AvklarSykdomLøser(connection: DBConnection) : AvklaringsbehovsLøser<AvklarSykdomLøsning> {

    private val repositoryProvider = RepositoryProvider(connection)
    private val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
    private val sykdomRepository = repositoryProvider.provide<SykdomRepository>()
    private val yrkersskadeRepository = repositoryProvider.provide<YrkesskadeRepository>()
    private val log = LoggerFactory.getLogger(javaClass)

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: AvklarSykdomLøsning): LøsningsResultat {
        val behandling = behandlingRepository.hent(kontekst.kontekst.behandlingId)
        val yrkesskadeGrunnlag = yrkersskadeRepository.hentHvisEksisterer(behandling.id)

        /* midlertidig, inntil frontend er over på sykdomsvurdering */
        val nyeSykdomsvurderinger = when {
            løsning.sykdomsvurderinger != null -> løsning.sykdomsvurderinger
            else -> listOf(løsning.sykdomsvurdering!!)
        }
            .map { it.toSykdomsvurdering(kontekst.bruker) }
            .let {
                SykdomGrunnlag(
                    id = null,
                    sykdomsvurderinger = it,
                    yrkesskadevurdering = null,
                ).somSykdomsvurderingstidslinje(LocalDate.MIN)
            }


        val eksisterendeSykdomsvurderinger = behandling.forrigeBehandlingId
            ?.let { sykdomRepository.hentHvisEksisterer(it) }
            ?.somSykdomsvurderingstidslinje(LocalDate.MIN)
            ?: Tidslinje()

        val gjeldendeVurderinger = eksisterendeSykdomsvurderinger
            .kombiner(nyeSykdomsvurderinger, StandardSammenslåere.prioriterHøyreSideCrossJoin())
            .toList()
            .map { it.verdi }

        validerSykdomOgYrkesskadeKonsistens(nyeSykdomsvurderinger, yrkesskadeGrunnlag)

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
        yrkesskadeGrunnlag: YrkesskadeGrunnlag?
    ) {
        val harYrkesskade = yrkesskadeGrunnlag?.yrkesskader?.harYrkesskade() == true
        sykdomLøsning.forEach {
            if (!it.verdi.erKonsistentForSykdom(harYrkesskade)) {
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
