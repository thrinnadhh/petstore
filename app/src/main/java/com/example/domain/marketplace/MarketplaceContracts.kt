package com.example.domain.marketplace

@JvmInline
value class Money private constructor(val paise: Long) : Comparable<Money> {
    init {
        require(paise >= 0) { "Money cannot be negative." }
    }

    override fun compareTo(other: Money): Int = paise.compareTo(other.paise)

    companion object {
        val ZERO = Money(0)

        fun ofPaise(paise: Long): Money = Money(paise)

        fun ofRupees(rupees: Long): Money = Money(Math.multiplyExact(rupees, 100))
    }
}

enum class MarketplaceRole {
    CUSTOMER,
    MERCHANT,
    CAPTAIN,
    SUPER_ADMIN
}

enum class AuthenticationFactor {
    PHONE_OTP,
    EMAIL_OTP,
    EMAIL_PASSWORD,
    TOTP
}

data class RoleAuthPolicy(
    val role: MarketplaceRole,
    val acceptedPrimaryFactors: Set<AuthenticationFactor>,
    val requiredSecondaryFactors: Set<AuthenticationFactor>,
    val requiresAdminApproval: Boolean,
    val requiresKycApproval: Boolean
) {
    init {
        require(acceptedPrimaryFactors.isNotEmpty()) {
            "At least one primary authentication factor is required."
        }
    }
}

data class RoleAccessEvidence(
    val verifiedFactors: Set<AuthenticationFactor>,
    val isAdminApproved: Boolean,
    val isKycApproved: Boolean
)

data class RoleAccessDecision(
    val isPrimaryFactorMissing: Boolean,
    val missingSecondaryFactors: Set<AuthenticationFactor>,
    val needsAdminApproval: Boolean,
    val needsKycApproval: Boolean
) {
    val isAllowed: Boolean
        get() = !isPrimaryFactorMissing &&
            missingSecondaryFactors.isEmpty() &&
            !needsAdminApproval &&
            !needsKycApproval
}

enum class CityLaunchStatus {
    DRAFT,
    PILOT,
    ACTIVE,
    PAUSED
}

enum class MarketplaceService {
    PRODUCTS,
    GROOMING,
    VETERINARY
}

enum class PaymentMethod {
    COD,
    ONLINE
}

enum class DeliveryProviderType {
    PETSTORE_FLEET,
    THIRD_PARTY
}

data class CityLaunchConfiguration(
    val cityId: String,
    val displayName: String,
    val status: CityLaunchStatus,
    val enabledServices: Set<MarketplaceService>,
    val enabledPaymentMethods: Set<PaymentMethod>,
    val enabledDeliveryProviders: Set<DeliveryProviderType>,
    val codLimit: Money?
) {
    init {
        require(cityId.isNotBlank()) { "City ID is required." }
        require(displayName.isNotBlank()) { "City display name is required." }
        require(PaymentMethod.COD !in enabledPaymentMethods || codLimit != null) {
            "A COD limit is required when COD is enabled."
        }
    }
}

data class CustomerCheckoutPolicy(
    val isGuestBrowsingEnabled: Boolean,
    val acceptedVerificationFactors: Set<AuthenticationFactor>
) {
    init {
        require(acceptedVerificationFactors.isNotEmpty()) {
            "Checkout requires at least one verified authentication factor."
        }
    }
}

data class CustomerCheckoutIdentity(
    val fullName: String,
    val mobileNumber: String,
    val deliveryAddress: String,
    val email: String?,
    val verifiedFactors: Set<AuthenticationFactor>
)

enum class SettlementTrigger {
    FULFILLMENT_CONFIRMED
}

data class SettlementPolicy(
    val platformCollectsOnlinePayments: Boolean,
    val merchantSettlementTrigger: SettlementTrigger,
    val requiresCodReconciliation: Boolean
)

object MarketplacePolicyDefaults {
    val customerAuth = RoleAuthPolicy(
        role = MarketplaceRole.CUSTOMER,
        acceptedPrimaryFactors = setOf(
            AuthenticationFactor.PHONE_OTP,
            AuthenticationFactor.EMAIL_OTP
        ),
        requiredSecondaryFactors = emptySet(),
        requiresAdminApproval = false,
        requiresKycApproval = false
    )

    val merchantAuth = RoleAuthPolicy(
        role = MarketplaceRole.MERCHANT,
        acceptedPrimaryFactors = setOf(AuthenticationFactor.PHONE_OTP),
        requiredSecondaryFactors = emptySet(),
        requiresAdminApproval = true,
        requiresKycApproval = false
    )

    val captainAuth = RoleAuthPolicy(
        role = MarketplaceRole.CAPTAIN,
        acceptedPrimaryFactors = setOf(AuthenticationFactor.PHONE_OTP),
        requiredSecondaryFactors = emptySet(),
        requiresAdminApproval = true,
        requiresKycApproval = true
    )

    val superAdminAuth = RoleAuthPolicy(
        role = MarketplaceRole.SUPER_ADMIN,
        acceptedPrimaryFactors = setOf(AuthenticationFactor.EMAIL_PASSWORD),
        requiredSecondaryFactors = setOf(AuthenticationFactor.TOTP),
        requiresAdminApproval = false,
        requiresKycApproval = false
    )

    val customerCheckout = CustomerCheckoutPolicy(
        isGuestBrowsingEnabled = true,
        acceptedVerificationFactors = setOf(
            AuthenticationFactor.PHONE_OTP,
            AuthenticationFactor.EMAIL_OTP
        )
    )

    val settlement = SettlementPolicy(
        platformCollectsOnlinePayments = true,
        merchantSettlementTrigger = SettlementTrigger.FULFILLMENT_CONFIRMED,
        requiresCodReconciliation = true
    )
}
