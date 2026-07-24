package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.register.sykepengemaksdato

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.sykepengemaksdato.MaksdatoHendelse
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.sykepengemaksdato.MaksdatoHendelseKilde
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.sykepengemaksdato.SykepengeMaksdatoRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.Factory

class SykepengeMaksdatoRepositoryImpl(private val connection: DBConnection) : SykepengeMaksdatoRepository {

    companion object : Factory<SykepengeMaksdatoRepositoryImpl> {
        override fun konstruer(connection: DBConnection): SykepengeMaksdatoRepositoryImpl {
            return SykepengeMaksdatoRepositoryImpl(connection)
        }
    }

    override fun lagre(maksdatoHendelse: MaksdatoHendelse, person: Person) {
        val query = """
            INSERT INTO SYKEPENGE_MAKSDATO (PERSON_ID, MAKSDATO, KILDE) VALUES (?, ?, ?)
ON CONFLICT (PERSON_ID) DO UPDATE SET MAKSDATO = EXCLUDED.MAKSDATO, KILDE = EXCLUDED.KILDE

        """.trimIndent()

        connection.execute(query) {
            setParams {
                setLong(1, person.id.id)
                setLocalDate(2, maksdatoHendelse.foreløpigMaksdato)
                setEnumName(3, maksdatoHendelse.kilde)
            }
        }
    }

    override fun hentHvisEksisterer(person: Person): MaksdatoHendelse? {
        val query = """
            SELECT * FROM SYKEPENGE_MAKSDATO WHERE PERSON_ID = ?
        """.trimIndent()
        return connection.queryFirstOrNull(query) {
            setParams {
                setLong(1, person.id.id)
            }
            setRowMapper { row ->
                MaksdatoHendelse(
                    personId = person.id,
                    foreløpigMaksdato = row.getLocalDate("MAKSDATO"),
                    kilde = row.getEnum<MaksdatoHendelseKilde>("KILDE")
                )
            }
        }
    }

    override fun kopier(
        fraBehandling: BehandlingId,
        tilBehandling: BehandlingId
    ) {
        // Skal ikke gjøres, maksdato lagres på person og ikke på behandling
    }

    override fun slett(behandlingId: BehandlingId) {
        // Skal ikke gjøres
    }
}