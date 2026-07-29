package com.example.domain.marketplace

import kotlinx.coroutines.flow.Flow

interface MarketplaceConfigurationRepository {
    fun observeCities(): Flow<List<CityLaunchConfiguration>>

    suspend fun getCity(cityId: String): CityLaunchConfiguration?

    suspend fun getRoleAuthPolicy(role: MarketplaceRole): RoleAuthPolicy

    suspend fun getCustomerCheckoutPolicy(): CustomerCheckoutPolicy

    suspend fun getSettlementPolicy(): SettlementPolicy
}

interface MarketplaceAdminConfigurationRepository {
    suspend fun saveCity(configuration: CityLaunchConfiguration)
}

