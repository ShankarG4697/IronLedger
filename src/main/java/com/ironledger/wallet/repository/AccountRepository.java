package com.ironledger.wallet.repository;

import com.ironledger.wallet.entity.Account;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Account entity operations.
 * Provides CRUD operations and custom queries for account management.
 */
@Repository
public interface AccountRepository extends JpaRepository<Account, UUID> {

    /**
     * Find an account by ID with pessimistic write lock for concurrent balance updates.
     * Use this method when you need to update the account balance to prevent race conditions.
     *
     * @param id the account ID
     * @return Optional containing the locked account if found
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.id = :id")
    Optional<Account> findByIdForUpdate(@Param("id") UUID id);

    /**
     * Find all accounts for a specific user.
     *
     * @param userId the user ID
     * @return list of accounts belonging to the user
     */
    List<Account> findByUserId(UUID userId);

    /**
     * Find all active accounts for a specific user.
     *
     * @param userId the user ID
     * @param status the account status
     * @return list of active accounts
     */
    List<Account> findByUserIdAndStatus(UUID userId, Integer status);

    /**
     * Find a specific user's account in a given currency.
     *
     * @param userId   the user ID
     * @param currency the currency code (e.g., USD, EUR)
     * @return Optional containing the account if found
     */
    Optional<Account> findByUserIdAndCurrency(UUID userId, String currency);

    /**
     * Check if a user already has an account in a specific currency.
     *
     * @param userId   the user ID
     * @param currency the currency code
     * @return true if account exists, false otherwise
     */
    boolean existsByUserIdAndCurrency(UUID userId, String currency);

    /**
     * Find all accounts by status with pagination.
     * Use pagination to avoid loading large datasets into memory.
     *
     * @param status   the account status
     * @param pageable pagination and sorting parameters
     * @return page of accounts with the given status
     */
    Page<Account> findByStatus(Integer status, Pageable pageable);

    /**
     * Count total accounts for a user.
     *
     * @param userId the user ID
     * @return number of accounts
     */
    long countByUserId(UUID userId);

    /**
     * Get total available balance across all active accounts for a user in a specific currency.
     *
     * @param userId   the user ID
     * @param currency the currency code
     * @param status   the account status
     * @return sum of available balances
     */
    @Query("SELECT COALESCE(SUM(a.balanceAvailable), 0) FROM Account a " +
           "WHERE a.userId = :userId AND a.currency = :currency AND a.status = :status")
    Long getTotalAvailableBalance(@Param("userId") UUID userId, 
                                   @Param("currency") String currency, 
                                   @Param("status") Integer status);

    /**
     * Find accounts with low balance (below a threshold) with pagination.
     * Use pagination to avoid loading large datasets into memory.
     *
     * @param threshold the minimum balance threshold
     * @param status    the account status
     * @param pageable  pagination and sorting parameters
     * @return page of accounts below the threshold
     */
    @Query("SELECT a FROM Account a WHERE a.balanceAvailable < :threshold AND a.status = :status")
    Page<Account> findAccountsWithLowBalance(@Param("threshold") Long threshold,
                                              @Param("status") Integer status,
                                              Pageable pageable);

    /**
     * Find all accounts for a user with pessimistic lock for batch updates.
     *
     * @param userId the user ID
     * @return list of locked accounts
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.userId = :userId")
    List<Account> findByUserIdForUpdate(@Param("userId") UUID userId);
}