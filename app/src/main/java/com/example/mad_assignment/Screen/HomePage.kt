package com.example.mad_assignment.Screen
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.mad_assignment.Data.Package
import com.example.mad_assignment.R


@Composable
fun PackageCard(packageData: Package, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {


//           AsyncImage(
//                model = packageData.imageUrl, // Use the imageUrl from your data object
//                contentDescription = "Image of ${packageData.packagesName}",
//                placeholder = painterResource(id = R.drawable.placeholder_background),
//                error = painterResource(id = R.drawable.error_placeholder), // For when loading fails
//                contentScale = ContentScale.Crop,
//                modifier = Modifier.fillMaxSize()
//            )

            // 2. A gradient scrim for text readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black),
                            startY = 300f // Adjust this value to control the gradient start
                        )
                    )
            )

            // 3. The content at the bottom
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart) // Aligns the Row to the bottom
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Location Name
                Text(
                    text = packageData.packagesName,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f) // Takes up all available space
                )

                // Spacer to give a little room
                Spacer(modifier = Modifier.width(8.dp))

                // Star Icon
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Rating Star",
                    tint = Color(0xFFFFC700) // A nice gold/yellow color
                )

                // Rating Text
                Text(
                    text = packageData.review.toString(),
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PackageCardPreview() {
    val samplePackage = _root_ide_package_.com.example.mad_assignment.Data.Package(
        packagesId = "p1",
        packagesName = "Genting Highland",
        review = 4.9,
        imageUrl = "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSKAYs_vpLM3So9qcw3Z80A0ubonxUhBQOCVg&s" // Preview will use the placeholder since the URL is empty
    )
    PackageCard(
        packageData = samplePackage,
        modifier = Modifier.size(width = 220.dp, height = 280.dp)
    )
}

