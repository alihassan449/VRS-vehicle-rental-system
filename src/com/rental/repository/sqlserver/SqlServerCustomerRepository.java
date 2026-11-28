package com.rental.repository.sqlserver;

import com.rental.model.Customer;
import com.rental.repository.InMemoryCustomerRepository;

/** Customers kept in memory (fast reads) and written to SQL Server on every save. */
public class SqlServerCustomerRepository extends InMemoryCustomerRepository {

    private final SqlServerDatabase db;

    public SqlServerCustomerRepository(SqlServerDatabase db) {
        this.db = db;
    }

    @Override
    public void save(Customer customer) {
        db.saveCustomer(customer);   // database first: if it fails, nothing is added to memory
        super.save(customer);
    }

    /** Used while loading from the database (no write back). */
    public void cache(Customer customer) {
        super.save(customer);
    }
}
