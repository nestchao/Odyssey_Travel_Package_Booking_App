//package com.example.mad_assignment
//
//import androidx.lifecycle.ViewModel
//import com.example.mad_assignment.data.model.TravelPackage
//import com.google.firebase.ktx.Firebase
//import com.google.firebase.firestore.ktx.firestore
//import kotlinx.coroutines.tasks.await
//
//class MainViewModel: ViewModel() {
//    private val db = Firebase.firestore
//    suspend fun getAllPackages(): List<TravelPackage>{
//        val travelPackageList = mutableListOf<TravelPackage>()
//        try{
//            val snapshot = db.collection("TravelPackage").get().await()
//            for (document in snapshot.documents){
//                val travelPackage = document.toObject(TravelPackage::class.java)
//                if(travelPackage != null){
//                    travelPackage.packageId = document.id
//                    travelPackageList.add(travelPackage)
//                }
//            }
//        }catch (e: Exception) {
//
//        }
//        return travelPackageList
//    }
//}