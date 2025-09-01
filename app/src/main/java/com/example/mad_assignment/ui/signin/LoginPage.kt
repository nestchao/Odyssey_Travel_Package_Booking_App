package com.example.mad_assignment.ui.signin

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.navigation.NavController
import com.example.mad_assignment.R


@Composable
fun LoginPage(navController: NavController){
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ){
        Image(
            painter = painterResource(R.drawable.generated_image_august_06__2025___6_03pm_removebg_preview),
            contentDescription = "Travel Illustration",
            modifier = Modifier.height(200.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 2. "Sign In" Header Text
        Text(
            text = "Sign In",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 3. Email Text Field
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 4. Password Text Field
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            // This part handles the password visibility toggle
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )

        // 5. "Forgot Password" Link
        TextButton(
            onClick = { /* TODO: Handle Forgot Password */ },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Forgot Password")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 6. Sign In Button
        Button(
            onClick = {
                navController.navigate("home")
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Sign In")
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 7. "Or" Divider
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Divider(modifier = Modifier.weight(1f))
            Text("Or", color = Color.Gray)
            Divider(modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(24.dp))

        // This is a placeholder for the social login button (e.g., Google)
        OutlinedButton(
            onClick = { /* TODO: Handle Google Sign In */ },
            shape = CircleShape,
            modifier = Modifier.size(50.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            // You would put the Google 'G' icon here
            // For now, it's just a circle
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 8. "Don't have account? Sign Up" Link
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Don't have account?")
            TextButton(onClick = { /* TODO: Navigate to Sign Up screen */ }) {
                Text("Sign Up")
            }
        }
    }
}