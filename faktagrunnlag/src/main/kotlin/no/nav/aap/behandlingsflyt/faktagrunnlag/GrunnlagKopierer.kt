package no.nav.aap.behandlingsflyt.faktagrunnlag

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.barnetillegg.BarnetilleggRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.BeregningsgrunnlagRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.PliktkortRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.InstitusjonsoppholdRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.BarnRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.UføreRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.YrkesskadeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne.ArbeidsevneRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.BarnVurderingRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningVurderingRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.BistandRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.MeldepliktRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykepengerErstatningRepository
import no.nav.aap.verdityper.sakogbehandling.BehandlingId

/**
 * Har som ansvar å sette i stand en behandling etter opprettelse
 *
 * Knytter alle opplysninger fra forrige til den nye i en immutable state
 */
class GrunnlagKopierer(connection: DBConnection) {

    private val vilkårsresultatRepository: VilkårsresultatRepository = VilkårsresultatRepository(connection)
    private val personopplysningRepository = PersonopplysningRepository(connection)
    private val yrkesskadeRepository = YrkesskadeRepository(connection)
    private val sykdomRepository = SykdomRepository(connection)
    private val studentRepository = StudentRepository(connection)
    private val bistandRepository = BistandRepository(connection)
    private val meldepliktRepository = MeldepliktRepository(connection)
    private val sykepengerErstatningRepository = SykepengerErstatningRepository(connection)
    private val arbeidsevneRepository = ArbeidsevneRepository(connection)
    private val uføreRepository = UføreRepository(connection)
    private val pliktkortRepository = PliktkortRepository(connection)
    private val underveisRepository = UnderveisRepository(connection)
    private val barnRepository = BarnRepository(connection)
    private val barnetilleggRepository = BarnetilleggRepository(connection)
    private val barnVurderingRepository = BarnVurderingRepository(connection)
    private val beregningsgrunnlagRepository = BeregningsgrunnlagRepository(connection)
    private val beregningVurderingRepository = BeregningVurderingRepository(connection)
    private val institusjonsoppholdRepository = InstitusjonsoppholdRepository(connection)

    fun overfør(fraBehandlingId: BehandlingId, tilBehandlingId: BehandlingId) {
        require(fraBehandlingId != tilBehandlingId)

        vilkårsresultatRepository.kopier(fraBehandlingId, tilBehandlingId)
        personopplysningRepository.kopier(fraBehandlingId, tilBehandlingId)
        yrkesskadeRepository.kopier(fraBehandlingId, tilBehandlingId)
        sykdomRepository.kopier(fraBehandlingId, tilBehandlingId)
        studentRepository.kopier(fraBehandlingId, tilBehandlingId)
        bistandRepository.kopier(fraBehandlingId, tilBehandlingId)
        meldepliktRepository.kopier(fraBehandlingId, tilBehandlingId)
        sykepengerErstatningRepository.kopier(fraBehandlingId, tilBehandlingId)
        arbeidsevneRepository.kopier(fraBehandlingId, tilBehandlingId)
        uføreRepository.kopier(fraBehandlingId, tilBehandlingId)
        pliktkortRepository.kopier(fraBehandlingId, tilBehandlingId)
        underveisRepository.kopier(fraBehandlingId, tilBehandlingId)
        barnRepository.kopier(fraBehandlingId, tilBehandlingId)
        barnetilleggRepository.kopier(fraBehandlingId, tilBehandlingId)
        barnVurderingRepository.kopier(fraBehandlingId, tilBehandlingId)
        beregningVurderingRepository.kopier(fraBehandlingId, tilBehandlingId)
        beregningsgrunnlagRepository.kopier(fraBehandlingId, tilBehandlingId)
        institusjonsoppholdRepository.kopier(fraBehandlingId, tilBehandlingId)
    }
}
