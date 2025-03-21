package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarBistandsbehovLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.BistandGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.BistandRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.tidslinje.StandardSammenslåere
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.LocalDate

class AvklarBistandLøser(val connection: DBConnection) :
    AvklaringsbehovsLøser<AvklarBistandsbehovLøsning> {

    private val repositoryProvider = RepositoryProvider(connection)
    private val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
    private val bistandRepository = repositoryProvider.provide<BistandRepository>()

    override fun løs(
        kontekst: AvklaringsbehovKontekst,
        løsning: AvklarBistandsbehovLøsning
    ): LøsningsResultat {
        val behandling = behandlingRepository.hent(kontekst.kontekst.behandlingId)

        val nyesteSykdomsvurdering = repositoryProvider.provide<SykdomRepository>().hentHvisEksisterer(behandling.id)
            ?.sykdomsvurderinger?.maxByOrNull { it.opprettet }

        val bistandsVurdering = løsning.bistandsVurdering.tilBistandVurdering(
            kontekst.bruker,
            nyesteSykdomsvurdering?.vurderingenGjelderFra
        )

        val eksisterendeBistandsvurderinger = behandling.forrigeBehandlingId
            ?.let { bistandRepository.hentHvisEksisterer(it) }
            ?.somBistandsvurderingstidslinje(LocalDate.MIN)
            ?: Tidslinje()

        val ny = bistandsVurdering.let {
            BistandGrunnlag(
                id = null,
                vurderinger = listOf(it),
            ).somBistandsvurderingstidslinje(LocalDate.MIN)
        }

        val gjeldende = eksisterendeBistandsvurderinger
            .kombiner(ny, StandardSammenslåere.prioriterHøyreSideCrossJoin())
            .toList().map { it.verdi }

        bistandRepository.lagre(
            behandlingId = behandling.id,
            bistandsvurderinger = gjeldende
        )

        return LøsningsResultat(
            begrunnelse = bistandsVurdering.begrunnelse
        )
    }

    override fun forBehov(): Definisjon {
        return Definisjon.AVKLAR_BISTANDSBEHOV
    }
}
