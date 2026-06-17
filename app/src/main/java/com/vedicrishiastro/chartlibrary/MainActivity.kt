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
import androidx.compose.foundation.layout.navigationBarsPadding
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
import com.vedicrishiastro.vedicchart.compose.VedicTransitPlanet

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
    val transitPlanets = remember { houseTransitsToChartPlanets(DUMMY_HOUSE_TRANSITS) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()).navigationBarsPadding()
            .padding(PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 24.dp)),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ChartSection("North Indian", ChartStyle.NORTH, ChartTheme.temple(), houses, transitPlanets)
        ChartSection("South Indian", ChartStyle.SOUTH, ChartTheme.temple(), houses, transitPlanets)
        ChartSection("East Indian", ChartStyle.EAST, ChartTheme.temple(), houses, transitPlanets)
    }
}

@Composable
private fun ChartSection(
    title: String,
    chartStyle: ChartStyle,
    chartTheme: ChartTheme,
    houses: List<ZodiacHouse>,
    transitPlanets: List<VedicTransitPlanet>,
) {
    var selectedPlanet by remember { mutableStateOf<VedicPlanetSelection?>(null) }
    var usePlanetIcons by remember { mutableStateOf(false) }
    var usePreviousChartColors by remember { mutableStateOf(false) }
    var is3dView by remember { mutableStateOf(true) }
    val effectiveChartTheme = remember(chartTheme, usePreviousChartColors) {
        if (usePreviousChartColors) {
            chartTheme.withPreviousHouseColors()
        } else {
            chartTheme
        }
    }

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
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = "Previous chart colors")
        Switch(
            checked = usePreviousChartColors,
            onCheckedChange = { usePreviousChartColors = it },
        )
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = "3D drag view")
        Switch(
            checked = is3dView,
            onCheckedChange = { is3dView = it },
        )
    }
    VedicChart(
        houses = houses,
        chartStyle = chartStyle,
        chartTheme = effectiveChartTheme,
        usePlanetIcons = usePlanetIcons,
        is3dView = is3dView,
        transitPlanets = transitPlanets,
        onPlanetSelected = { selectedPlanet = it },
        modifier = Modifier,
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

private data class HouseTransit(
    val planetName: String,
    val currentNatalHouseFromLagna: Int,
)

private fun houseTransitsToChartPlanets(transits: List<HouseTransit>): List<VedicTransitPlanet> {
    return transits.mapNotNull { transit ->
        val houseIndex = transit.currentNatalHouseFromLagna - 1
        if (houseIndex !in 0 until 12) return@mapNotNull null
        VedicTransitPlanet(
            planetName = transit.planetName,
            planetLabel = planetShortLabel(transit.planetName),
            houseIndex = houseIndex,
        )
    }
}

private fun planetShortLabel(planetName: String): String {
    return when (planetName.trim().uppercase()) {
        "SUN", "SURYA" -> "Su"
        "MOON", "CHANDRA" -> "Mo"
        "MARS", "MANGAL" -> "Ma"
        "MERCURY", "BUDH" -> "Me"
        "JUPITER", "GURU", "BRIHASPATI" -> "Ju"
        "VENUS", "SHUKRA" -> "Ve"
        "SATURN", "SHANI" -> "Sa"
        "RAHU" -> "Ra"
        "KETU" -> "Ke"
        else -> planetName.trim().take(2).replaceFirstChar { it.uppercase() }
    }
}

private fun ChartTheme.withPreviousHouseColors(): ChartTheme {
    val previousTheme = ChartTheme.light()
    return ChartTheme.Builder()
        .backgroundColor(backgroundColor)
        .borderColor(borderColor)
        .signTextColor(signTextColor)
        .planetTextColor(planetTextColor)
        .accentColor(accentColor)
        .houseColors(*previousTheme.houseColors)
        .selectedHouseColor(previousTheme.selectedHouseColor)
        .borderWidth(borderWidth)
        .signTextSizeSp(signTextSizeSp)
        .planetTextSizeSp(planetTextSizeSp)
        .showSignNames(shouldShowSignNames())
        .build()
}

private val DUMMY_HOUSE_TRANSITS = listOf(
    HouseTransit(planetName = "Sun", currentNatalHouseFromLagna = 1),
    HouseTransit(planetName = "Moon", currentNatalHouseFromLagna = 1),
    HouseTransit(planetName = "Mars", currentNatalHouseFromLagna = 12),
    HouseTransit(planetName = "Mercury", currentNatalHouseFromLagna = 2),
    HouseTransit(planetName = "Jupiter", currentNatalHouseFromLagna = 2),
    HouseTransit(planetName = "Venus", currentNatalHouseFromLagna = 3),
    HouseTransit(planetName = "Saturn", currentNatalHouseFromLagna = 11),
    HouseTransit(planetName = "Rahu", currentNatalHouseFromLagna = 10),
    HouseTransit(planetName = "Ketu", currentNatalHouseFromLagna = 4),
)

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
