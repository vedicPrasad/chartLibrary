package com.vedicrishiastro.chartlibrary

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.vedicrishiastro.vedicchart.ChartStyle
import com.vedicrishiastro.vedicchart.ChartTheme
import com.vedicrishiastro.vedicchart.ZodiacHouse
import com.vedicrishiastro.vedicchart.compose.VedicChart
import com.vedicrishiastro.vedicchart.compose.VedicPlanetSelection

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = PremiumColorScheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    ChartDemoScreen()
                }
            }
        }
    }
}

@Composable
private fun ChartDemoScreen() {
    val houses = remember { ZodiacHouse.fromJson(DUMMY_CHART_JSON) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 24.dp)),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ChartSection("North Indian", ChartStyle.NORTH, ChartTheme.light(), houses)
//        ChartSection("South Indian", ChartStyle.SOUTH, ChartTheme.temple(), houses)
//        ChartSection("East Indian", ChartStyle.EAST, ChartTheme.dark(), houses)
    }
}

@Composable
private fun ChartSection(
    title: String,
    chartStyle: ChartStyle,
    chartTheme: ChartTheme,
    houses: List<ZodiacHouse>,
) {
    var selectedPlanet by remember { mutableStateOf<VedicPlanetSelection?>(null) }
    var usePlanetIcons by remember { mutableStateOf(false) }

    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 8.dp),
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = "Use planet icons")
        Switch(
            checked = usePlanetIcons,
            onCheckedChange = { usePlanetIcons = it },
        )
    }
    VedicChart(
        houses = houses,
        chartStyle = chartStyle,
        chartTheme = chartTheme,
        usePlanetIcons = usePlanetIcons,
        onPlanetSelected = { selectedPlanet = it },
        modifier = Modifier
            .fillMaxWidth()
            .height(340.dp),
    )

    selectedPlanet?.let { planet ->
        AlertDialog(
            onDismissRequest = { selectedPlanet = null },
            title = {
                Text(text = planet.planetName)
            },
            text = {
                Text(
                    text = "${planet.planetName} is placed in ${planet.house.signName} (${planet.house.sign}) at house ${planet.houseIndex + 1}.",
                )
            },
            confirmButton = {
                Button(onClick = { selectedPlanet = null }) {
                    Text(text = "Done")
                }
            },
        )
    }
}

private val PremiumColorScheme = lightColorScheme(
    primary = Color(0xFFC18426),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF9A202D),
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFFFFFDF8),
    onBackground = Color(0xFF2B231C),
    surface = Color(0xFFFFFDF8),
    onSurface = Color(0xFF2B231C),
    surfaceVariant = Color(0xFFF4ECE1),
    onSurfaceVariant = Color(0xFF574536),
    outline = Color(0xFFB79D7B),
)

private const val DUMMY_CHART_JSON = """
[{"sign":2,"sign_name":"Taurus","planet":[],"planet_small":[],"planet_degree":[]},{"sign":3,"sign_name":"Gemini","planet":[],"planet_small":[],"planet_degree":[]},{"sign":4,"sign_name":"Cancer","planet":["MOON","RAHU"],"planet_small":["Mo ","Ra "],"planet_degree":[]},{"sign":5,"sign_name":"Leo","planet":[],"planet_small":[],"planet_degree":[]},{"sign":6,"sign_name":"Virgo","planet":[],"planet_small":[],"planet_degree":[]},{"sign":7,"sign_name":"Libra","planet":["MARS"],"planet_small":["Ma "],"planet_degree":[]},{"sign":8,"sign_name":"Scorpio","planet":[],"planet_small":[],"planet_degree":[]},{"sign":9,"sign_name":"Sagittarius","planet":[],"planet_small":[],"planet_degree":[]},{"sign":10,"sign_name":"Capricorn","planet":["KETU"],"planet_small":["Ke "],"planet_degree":[]},{"sign":11,"sign_name":"Aquarius","planet":["SUN"],"planet_small":["Su "],"planet_degree":[]},{"sign":12,"sign_name":"Pisces","planet":["MERCURY","JUPITER","VENUS"],"planet_small":["Me ","Ju ","Ve "],"planet_degree":[]},{"sign":1,"sign_name":"Aries","planet":["SATURN"],"planet_small":["Sa "],"planet_degree":[]}]
"""
