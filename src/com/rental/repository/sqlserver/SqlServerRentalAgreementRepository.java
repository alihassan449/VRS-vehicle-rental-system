package com.rental.repository.sqlserver;

import com.rental.model.RentalAgreement;
import com.rental.repository.InMemoryRentalAgreementRepository;

/** Rental agreements (with payments) kept in memory and written to SQL Server on every save. */
public class SqlServerRentalAgreementRepository extends InMemoryRentalAgreementRepository {

    private final SqlServerDatabase db;

    public SqlServerRentalAgreementRepository(SqlServerDatabase db) {
        this.db = db;
    }

    @Override
    public void save(RentalAgreement agreement) {
        db.saveAgreement(agreement);
        super.save(agreement);
    }

    public void cache(RentalAgreement agreement) {
        super.save(agreement);
    }
}
