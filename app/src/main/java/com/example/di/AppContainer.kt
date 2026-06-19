package com.example.di

import android.content.Context
import com.example.data.AppDatabase
import com.example.data.AuthRepository
import com.example.data.DemoAuthRepositoryImpl
import com.example.data.PawsRepository
import com.example.data.ProductionConfig
import com.example.data.SupabaseAuthRepositoryImpl
import com.example.domain.cart.AddToCartUseCase
import com.example.domain.cart.ClearCartUseCase
import com.example.domain.cart.RemoveFromCartUseCase
import com.example.domain.common.UuidIdGenerator
import com.example.domain.grooming.BookGroomingSlotUseCase
import com.example.domain.grooming.CancelGroomingBookingUseCase
import com.example.domain.grooming.UpdateGroomingBookingStatusUseCase
import com.example.domain.orders.PlaceOrderUseCase
import com.example.domain.orders.UpdateOrderStatusUseCase
import com.example.domain.vet.BookDoctorAppointmentUseCase
import com.example.domain.vet.CancelDoctorAppointmentUseCase

class AppContainer(context: Context) {
    val database: AppDatabase = AppDatabase.getDatabase(context.applicationContext)
    val repository: PawsRepository = PawsRepository(database.pawsDao())

    private val idGenerator = UuidIdGenerator()

    val authRepository: AuthRepository =
        if (ProductionConfig.IS_DEMO_MODE) {
            DemoAuthRepositoryImpl(database.pawsDao())
        } else {
            SupabaseAuthRepositoryImpl(database.pawsDao())
        }

    val addToCartUseCase = AddToCartUseCase()
    val removeFromCartUseCase = RemoveFromCartUseCase()
    val clearCartUseCase = ClearCartUseCase()
    val placeOrderUseCase = PlaceOrderUseCase(repository, idGenerator)

    val updateOrderStatusUseCase = UpdateOrderStatusUseCase(repository)
    val bookGroomingSlotUseCase = BookGroomingSlotUseCase(repository, idGenerator)
    val cancelGroomingBookingUseCase = CancelGroomingBookingUseCase(repository)
    val updateGroomingBookingStatusUseCase = UpdateGroomingBookingStatusUseCase(repository)
    val bookDoctorAppointmentUseCase = BookDoctorAppointmentUseCase(repository, idGenerator)
    val cancelDoctorAppointmentUseCase = CancelDoctorAppointmentUseCase(repository)
}
