package com.example

import com.example.data.fake.InMemoryMarketplaceConfigurationRepository
import com.example.domain.marketplace.AuthenticationFactor
import com.example.domain.marketplace.CheckoutAttempt
import com.example.domain.marketplace.CheckoutBlocker
import com.example.domain.marketplace.CityLaunchConfiguration
import com.example.domain.marketplace.CityLaunchStatus
import com.example.domain.marketplace.CustomerCheckoutIdentity
import com.example.domain.marketplace.DeliveryProviderType
import com.example.domain.marketplace.EvaluateCheckoutEligibilityUseCase
import com.example.domain.marketplace.EvaluateRoleAccessUseCase
import com.example.domain.marketplace.GetCustomerVisibleCitiesUseCase
import com.example.domain.marketplace.MarketplacePolicyDefaults
import com.example.domain.marketplace.MarketplaceService
import com.example.domain.marketplace.Money
import com.example.domain.marketplace.PaymentMethod
import com.example.domain.marketplace.RoleAccessEvidence
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MarketplaceFoundationTest {
    private val checkoutEligibility = EvaluateCheckoutEligibilityUseCase()
    private val tirupati = CityLaunchConfiguration(
        cityId = "tirupati",
        displayName = "Tirupati",
        status = CityLaunchStatus.ACTIVE,
        enabledServices = MarketplaceService.entries.toSet(),
        enabledPaymentMethods = PaymentMethod.entries.toSet(),
        enabledDeliveryProviders = DeliveryProviderType.entries.toSet(),
        codLimit = Money.ofRupees(1_000)
    )

    @Test
    fun `only active cities are visible to customers`() {
        val draft = tirupati.copy(
            cityId = "bengaluru",
            displayName = "Bengaluru",
            status = CityLaunchStatus.DRAFT
        )

        val visible = GetCustomerVisibleCitiesUseCase()(listOf(draft, tirupati))

        assertEquals(listOf("tirupati"), visible.map { it.cityId })
    }

    @Test
    fun `admin can activate a city without changing customer code`() = runBlocking {
        val draft = tirupati.copy(
            cityId = "nellore",
            displayName = "Nellore",
            status = CityLaunchStatus.DRAFT
        )
        val repository = InMemoryMarketplaceConfigurationRepository(listOf(draft))

        repository.saveCity(draft.copy(status = CityLaunchStatus.ACTIVE))
        val visible = GetCustomerVisibleCitiesUseCase()(repository.observeCities().first())

        assertEquals(listOf("nellore"), visible.map { it.cityId })
    }

    @Test
    fun `guest can browse but incomplete identity cannot checkout`() {
        val attempt = validCheckoutAttempt().copy(
            identity = CustomerCheckoutIdentity(
                fullName = "",
                mobileNumber = "",
                deliveryAddress = "",
                email = null,
                verifiedFactors = emptySet()
            )
        )

        val result = checkoutEligibility(attempt)

        assertTrue(MarketplacePolicyDefaults.customerCheckout.isGuestBrowsingEnabled)
        assertFalse(result.isEligible)
        assertTrue(CheckoutBlocker.FULL_NAME_REQUIRED in result.blockers)
        assertTrue(CheckoutBlocker.MOBILE_NUMBER_REQUIRED in result.blockers)
        assertTrue(CheckoutBlocker.DELIVERY_ADDRESS_REQUIRED in result.blockers)
        assertTrue(CheckoutBlocker.VERIFICATION_REQUIRED in result.blockers)
    }

    @Test
    fun `verified customer can place online order above COD limit`() {
        val attempt = validCheckoutAttempt().copy(
            paymentMethod = PaymentMethod.ONLINE,
            orderTotal = Money.ofRupees(2_500)
        )

        val result = checkoutEligibility(attempt)

        assertTrue(result.isEligible)
    }

    @Test
    fun `customer can verify checkout by email while mobile remains required`() {
        val attempt = validCheckoutAttempt().copy(
            identity = CustomerCheckoutIdentity(
                fullName = "Ananya Rao",
                mobileNumber = "+919876543210",
                deliveryAddress = "Tirupati, Andhra Pradesh",
                email = "ananya@example.com",
                verifiedFactors = setOf(AuthenticationFactor.EMAIL_OTP)
            )
        )

        val result = checkoutEligibility(attempt)

        assertTrue(result.isEligible)
    }

    @Test
    fun `COD order above city limit is blocked`() {
        val attempt = validCheckoutAttempt().copy(orderTotal = Money.ofRupees(1_001))

        val result = checkoutEligibility(attempt)

        assertFalse(result.isEligible)
        assertEquals(setOf(CheckoutBlocker.COD_LIMIT_EXCEEDED), result.blockers)
    }

    @Test
    fun `captain requires phone approval and KYC`() {
        val evidence = RoleAccessEvidence(
            verifiedFactors = setOf(AuthenticationFactor.PHONE_OTP),
            isAdminApproved = false,
            isKycApproved = false
        )

        val decision = EvaluateRoleAccessUseCase()(MarketplacePolicyDefaults.captainAuth, evidence)

        assertFalse(decision.isAllowed)
        assertTrue(decision.needsAdminApproval)
        assertTrue(decision.needsKycApproval)
    }

    @Test
    fun `super admin requires password and TOTP`() {
        val evidence = RoleAccessEvidence(
            verifiedFactors = setOf(AuthenticationFactor.EMAIL_PASSWORD),
            isAdminApproved = true,
            isKycApproved = true
        )

        val decision = EvaluateRoleAccessUseCase()(MarketplacePolicyDefaults.superAdminAuth, evidence)

        assertFalse(decision.isAllowed)
        assertEquals(setOf(AuthenticationFactor.TOTP), decision.missingSecondaryFactors)
    }

    private fun validCheckoutAttempt(): CheckoutAttempt {
        return CheckoutAttempt(
            city = tirupati,
            service = MarketplaceService.PRODUCTS,
            paymentMethod = PaymentMethod.COD,
            orderTotal = Money.ofRupees(1_000),
            identity = CustomerCheckoutIdentity(
                fullName = "Ananya Rao",
                mobileNumber = "+919876543210",
                deliveryAddress = "Tirupati, Andhra Pradesh",
                email = null,
                verifiedFactors = setOf(AuthenticationFactor.PHONE_OTP)
            ),
            policy = MarketplacePolicyDefaults.customerCheckout
        )
    }
}
