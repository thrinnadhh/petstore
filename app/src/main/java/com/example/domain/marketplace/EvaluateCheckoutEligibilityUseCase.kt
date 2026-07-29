package com.example.domain.marketplace

data class CheckoutAttempt(
    val city: CityLaunchConfiguration,
    val service: MarketplaceService,
    val paymentMethod: PaymentMethod,
    val orderTotal: Money,
    val identity: CustomerCheckoutIdentity,
    val policy: CustomerCheckoutPolicy
)

enum class CheckoutBlocker {
    CITY_UNAVAILABLE,
    SERVICE_UNAVAILABLE,
    FULL_NAME_REQUIRED,
    MOBILE_NUMBER_REQUIRED,
    DELIVERY_ADDRESS_REQUIRED,
    EMAIL_ADDRESS_REQUIRED,
    VERIFICATION_REQUIRED,
    PAYMENT_METHOD_UNAVAILABLE,
    COD_LIMIT_EXCEEDED
}

data class CheckoutEligibility(val blockers: Set<CheckoutBlocker>) {
    val isEligible: Boolean
        get() = blockers.isEmpty()
}

class EvaluateCheckoutEligibilityUseCase {
    operator fun invoke(attempt: CheckoutAttempt): CheckoutEligibility {
        val blockers = buildSet {
            addAll(getCityBlockers(attempt))
            addAll(getIdentityBlockers(attempt))
            addAll(getPaymentBlockers(attempt))
        }
        return CheckoutEligibility(blockers)
    }

    private fun getCityBlockers(attempt: CheckoutAttempt): Set<CheckoutBlocker> = buildSet {
        if (attempt.city.status != CityLaunchStatus.ACTIVE) {
            add(CheckoutBlocker.CITY_UNAVAILABLE)
        }
        if (attempt.service !in attempt.city.enabledServices) {
            add(CheckoutBlocker.SERVICE_UNAVAILABLE)
        }
    }

    private fun getIdentityBlockers(attempt: CheckoutAttempt): Set<CheckoutBlocker> = buildSet {
        if (attempt.identity.fullName.isBlank()) {
            add(CheckoutBlocker.FULL_NAME_REQUIRED)
        }
        if (attempt.identity.mobileNumber.isBlank()) {
            add(CheckoutBlocker.MOBILE_NUMBER_REQUIRED)
        }
        if (attempt.identity.deliveryAddress.isBlank()) {
            add(CheckoutBlocker.DELIVERY_ADDRESS_REQUIRED)
        }
        if (
            AuthenticationFactor.EMAIL_OTP in attempt.identity.verifiedFactors &&
            attempt.identity.email.isNullOrBlank()
        ) {
            add(CheckoutBlocker.EMAIL_ADDRESS_REQUIRED)
        }
        if (attempt.identity.verifiedFactors.none(attempt.policy.acceptedVerificationFactors::contains)) {
            add(CheckoutBlocker.VERIFICATION_REQUIRED)
        }
    }

    private fun getPaymentBlockers(attempt: CheckoutAttempt): Set<CheckoutBlocker> = buildSet {
        if (attempt.paymentMethod !in attempt.city.enabledPaymentMethods) {
            add(CheckoutBlocker.PAYMENT_METHOD_UNAVAILABLE)
            return@buildSet
        }
        if (attempt.paymentMethod == PaymentMethod.COD && exceedsCodLimit(attempt)) {
            add(CheckoutBlocker.COD_LIMIT_EXCEEDED)
        }
    }

    private fun exceedsCodLimit(attempt: CheckoutAttempt): Boolean {
        val limit = attempt.city.codLimit ?: return true
        return attempt.orderTotal > limit
    }
}
