package com.example.mad_assignment.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// @Module tells Hilt this is a module that provides dependencies.
@Module
// @InstallIn specifies the scope of the dependencies. SingletonComponent means
// they will live as long as the application itself (i.e., they are singletons).
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    // @Provides tells Hilt that this function provides an instance of an object.
    @Provides
    // @Singleton ensures that Hilt creates only ONE instance of Firestore and reuses it.
    @Singleton
    fun provideFirestoreInstance(): FirebaseFirestore {
        // This is the code that Hilt will run to get a Firestore instance.
        return Firebase.firestore
    }

    // You will also need FirebaseAuth for booking, so let's provide it now.
    @Provides
    @Singleton
    fun provideAuthInstance(): FirebaseAuth {
        return Firebase.auth
    }
}