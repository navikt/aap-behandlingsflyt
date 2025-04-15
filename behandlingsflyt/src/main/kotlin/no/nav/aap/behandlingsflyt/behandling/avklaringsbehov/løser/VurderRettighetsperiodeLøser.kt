package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.VurderRettighetsperiodeLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.GrunnlagKopiererImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.sak.Status
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.komponenter.miljo.Miljø
import no.nav.aap.komponenter.miljo.MiljøKode
import no.nav.aap.lookup.repository.RepositoryProvider

class VurderRettighetsperiodeLøser(connection: DBConnection) :
    AvklaringsbehovsLøser<VurderRettighetsperiodeLøsning> {

    private val repositoryProvider = RepositoryProvider(connection)
    private val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
    private val sakRepository = repositoryProvider.provide<SakRepository>()
    private val grunnlagKopierer = GrunnlagKopiererImpl(connection)

    private val sakOgBehandlingService = SakOgBehandlingService(grunnlagKopierer, sakRepository, behandlingRepository)

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: VurderRettighetsperiodeLøsning): LøsningsResultat {
        if (Miljø.er() == MiljøKode.PROD) {
            throw UgyldigForespørselException("Kan kke overstyre virkningstidspunkt i prod ennå")
        }
        val behandling = behandlingRepository.hent(kontekst.kontekst.behandlingId)
        val sak = sakRepository.hent(kontekst.kontekst.sakId)

        if (behandling.status().erAvsluttet()) {
            throw UgyldigForespørselException("Kan ikke oppdatere rettighetsperioden etter at behandlingen er avsluttet")
        }
        if (sak.status() == Status.AVSLUTTET) {
            throw UgyldigForespørselException("Kan ikke oppdatere rettighetsperioden etter at saken er avsluttet")
        }
        val sluttDato = if (behandling.typeBehandling() == TypeBehandling.Førstegangsbehandling) {
            løsning.rettighetsperiodeVurdering.startDato.plusYears(1).minusDays(1)
        } else {
            sak.rettighetsperiode.tom
        }

        // TODO: Persistere ned begrunnelsen for å oppdatere rettighetsperioden
        sakOgBehandlingService.overstyrRettighetsperioden(
            sakId = sak.id,
            startDato = løsning.rettighetsperiodeVurdering.startDato,
            sluttDato = sluttDato
        )

        return LøsningsResultat("Vurdert rettighetsperiode")
    }

    override fun forBehov(): Definisjon {
        return Definisjon.VURDER_RETTIGHETSPERIODE
    }
}