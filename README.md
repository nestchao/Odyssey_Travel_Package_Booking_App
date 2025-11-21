# Odyssey Travel Package Booking App

## Description
Odyssey is a modern Android application designed to simplify the travel booking experience. Built with **Jetpack Compose** and **Kotlin**, it allows users to explore curated travel packages, manage bookings, and plan their trips with ease. The app integrates **Google Gemini AI** for intelligent assistance and uses **Firebase** for a robust backend.

## Features
- **Explore Packages**: Browse a wide range of travel packages with rich images and detailed itineraries.
- **Smart Search**: Filter packages by destination, price, and duration.
- **AI Assistant**: Integrated **Gemini AI** chatbot to assist with travel queries and recommendations.
- **User Accounts**: Secure authentication and profile management using **Firebase Auth**.
- **Booking System**: Seamless booking process with status tracking (Pending, Confirmed, etc.).
- **Cart & Wishlist**: Save packages for later or add them to your cart for checkout.
- **Trip Management**: View upcoming and past trips with detailed itinerary views.
- **Notifications**: Real-time updates on booking status and promotions via **Firebase Cloud Messaging**.
- **Location Services**: Location-based features using Google Play Services.

## Tech Stack
- **Language**: [Kotlin](https://kotlinlang.org/)
- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) (Material 3)
- **Architecture**: MVVM (Model-View-ViewModel) with Repository Pattern
- **Dependency Injection**: [Hilt](https://dagger.dev/hilt/)
- **Asynchronous Programming**: [Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) & Flow
- **Backend**: [Firebase](https://firebase.google.com/) (Firestore, Auth, Messaging, Analytics)
- **Image Loading**: [Coil](https://coil-kt.github.io/coil/)
- **Navigation**: [Jetpack Navigation Compose](https://developer.android.com/guide/navigation/navigation-compose)
- **AI Integration**: [Google Generative AI SDK](https://ai.google.dev/) (Gemini)
- **Local Storage**: SharedPreferences / DataStore (implied)

## Setup & Installation

### Prerequisites
- Android Studio Iguana or later
- JDK 11 or higher
- Android SDK API 34 (UpsideDownCake) or higher

### Steps
1. **Clone the Repository**
   ```bash
   git clone https://github.com/yourusername/odyssey-travel-app.git
   cd Odyssey_Travel_Package_Booking_App
   ```

2. **Configure Firebase**
   - Create a project in the [Firebase Console](https://console.firebase.google.com/).
   - Add an Android app with package name `com.example.mad_assignment`.
   - Download `google-services.json` and place it in the `app/` directory.
   - Enable **Authentication** (Email/Password, Google), **Firestore Database**, and **Cloud Messaging**.

3. **Configure Secrets**
   - Create a `local.properties` file in the root directory if it doesn't exist.
   - Add your Gemini API key:
     ```properties
     GEMINI_API_KEY=your_api_key_here
     ```

4. **Build and Run**
   - Open the project in Android Studio.
   - Sync Gradle files.
   - Select an emulator or connected device.
   - Click **Run** (Shift+F10).

## Architecture Overview
The app follows the **MVVM** architecture to ensure separation of concerns and testability:
- **Data Layer**: Repositories (`TravelPackageRepository`, `BookingRepository`) handle data operations from Firebase or local sources.
- **Domain/Model Layer**: Data classes (`TravelPackage`, `Booking`, `User`) define the core business objects.
- **UI Layer**: Jetpack Compose screens observe ViewModels (`MainViewModel`) to display UI states.

## Contributing
Contributions are welcome! Please follow these steps:
1. Fork the repository.
2. Create a new branch (`git checkout -b feature/AmazingFeature`).
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`).
4. Push to the branch (`git push origin feature/AmazingFeature`).
5. Open a Pull Request.

## License
Distributed under the MIT License. See `LICENSE` for more information.
