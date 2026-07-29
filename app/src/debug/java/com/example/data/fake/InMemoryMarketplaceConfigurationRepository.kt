package com.example.data.fake

import com.example.domain.marketplace.CityLaunchConfiguration
import com.example.domain.marketplace.CityLaunchStatus
import com.example.domain.marketplace.CustomerCheckoutPolicy
import com.example.domain.marketplace.DeliveryProviderType
import com.example.domain.marketplace.MarketplaceAdminConfigurationRepository
import com.example.domain.marketplace.MarketplaceConfigurationRepository
import com.example.domain.marketplace.MarketplacePolicyDefaults
import com.example.domain.marketplace.MarketplaceRole
import com.example.domain.marketplace.MarketplaceService
import com.example.domain.marketplace.Money
import com.example.domain.marketplace.PaymentMethod
import com.example.domain.marketplace.RoleAuthPolicy
import com.example.domain.marketplace.SettlementPolicy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class InMemoryMarketplaceConfigurationRepository(
    initialCities: List<CityLaunchConfiguration> = debugCities()
) : MarketplaceConfigurationRepository, MarketplaceAdminConfigurationRepository {
    private val cities = MutableStateFlow(initialCities.sortedBy { it.displayName.lowercase() })

    override fun observeCities(): Flow<List<CityLaunchConfiguration>> = cities.asStateFlow()

    override suspend fun getCity(cityId: String): CityLaunchConfiguration? {
        return cities.value.firstOrNull { it.cityId == cityId }
    }

    override suspend fun getRoleAuthPolicy(role: MarketplaceRole): RoleAuthPolicy {
        return when (role) {
            MarketplaceRole.CUSTOMER -> MarketplacePolicyDefaults.customerAuth
            MarketplaceRole.MERCHANT -> MarketplacePolicyDefaults.merchantAuth
            MarketplaceRole.CAPTAIN -> MarketplacePolicyDefaults.captainAuth
            MarketplaceRole.SUPER_ADMIN -> MarketplacePolicyDefaults.superAdminAuth
        }
    }

    override suspend fun getCustomerCheckoutPolicy(): CustomerCheckoutPolicy {
        return MarketplacePolicyDefaults.customerCheckout
    }

    override suspend fun getSettlementPolicy(): SettlementPolicy {
        return MarketplacePolicyDefaults.settlement
    }

    override suspend fun saveCity(configuration: CityLaunchConfiguration) {
        cities.value = (cities.value.filterNot { it.cityId == configuration.cityId } + configuration)
            .sortedBy { it.displayName.lowercase() }
    }
}

private fun debugCities(): List<CityLaunchConfiguration> = listOf(
    CityLaunchConfiguration(
        cityId = "tirupati",
        displayName = "Tirupati",
        status = CityLaunchStatus.ACTIVE,
        enabledServices = MarketplaceService.entries.toSet(),
        enabledPaymentMethods = PaymentMethod.entries.toSet(),
        enabledDeliveryProviders = DeliveryProviderType.entries.toSet(),
        codLimit = Money.ofRupees(1_000)
    ),
    CityLaunchConfiguration(
        cityId = "bengaluru",
        displayName = "Bengaluru",
        status = CityLaunchStatus.DRAFT,
        enabledServices = emptySet(),
        enabledPaymentMethods = setOf(PaymentMethod.ONLINE),
        enabledDeliveryProviders = emptySet(),
        codLimit = null
    )
)

