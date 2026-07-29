package com.example.domain.marketplace

class GetCustomerVisibleCitiesUseCase {
    operator fun invoke(cities: List<CityLaunchConfiguration>): List<CityLaunchConfiguration> {
        return cities
            .filter { it.status == CityLaunchStatus.ACTIVE }
            .sortedBy { it.displayName.lowercase() }
    }
}

