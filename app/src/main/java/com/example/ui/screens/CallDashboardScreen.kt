package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.cloud.CallerCategory
import com.example.data.model.AnalyticsChartBucket
import com.example.data.model.AnalyticsPeriod
import com.example.data.model.AppLanguage
import com.example.data.model.CallRoute
import com.example.data.model.formatDisplayNumber
import com.example.ui.components.ContactAvatar
import com.example.ui.components.formatCallDuration
import com.example.ui.theme.LocalNovaColors
import com.example.ui.viewmodel.DialerUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CallDashboardScreen(
    state: DialerUiState,
    onBack: () -> Unit,
    onSelectPeriod: (AnalyticsPeriod) -> Unit,
    onSearchNumberChange: (String) -> Unit,
    onOpenProfile: (String, String?) -> Unit,
    onCallRoute: (String, CallRoute, String?) -> Unit,
    onOpenTagCaller: (String, String?) -> Unit,
    onReportSpam: (String, String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val nova = LocalNovaColors.current
    val isBn = state.preferences.language == AppLanguage.BENGALI
    val summary = state.analyticsSummary

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp)
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("dashboard_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = if (isBn) "ফিরে যান" else "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isBn) "কল ড্যাশবোর্ড ও রিপোর্ট" else "Call Dashboard & Analytics",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = if (isBn) "লাইভ নম্বর সার্চ এবং কল বিশ্লেষণ" else "Number lookup, Daily/Weekly/Monthly reports",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(nova.sim1Container),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Analytics, contentDescription = null, tint = nova.sim1, modifier = Modifier.size(20.dp))
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 4.dp, bottom = 36.dp)
            ) {
                // 1. Live Number Search & Smart Caller ID Card
                item(key = "number_search") {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        color = nova.glassSurface,
                        border = BorderStroke(1.dp, nova.glassBorder.copy(alpha = 0.78f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Filled.VerifiedUser, contentDescription = null, tint = Color(0xFF0087FF), modifier = Modifier.size(19.dp))
                                Text(
                                    text = if (isBn) "নম্বর সার্চ (Smart Caller ID)" else "Number Search & Caller ID",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = state.numberSearchQuery,
                                onValueChange = onSearchNumberChange,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("dashboard_number_search_input"),
                                placeholder = {
                                    Text(if (isBn) "অচেনা নম্বর লিখে খুঁজুন (যেমন 017...)" else "Type any phone number (e.g. 017...)")
                                },
                                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = nova.gold) },
                                trailingIcon = {
                                    when {
                                        state.isSearchingNumber -> CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                        state.numberSearchQuery.isNotEmpty() -> IconButton(onClick = { onSearchNumberChange("") }) {
                                            Icon(Icons.Filled.Close, contentDescription = "Clear")
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(50),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    focusedBorderColor = nova.gold,
                                    unfocusedBorderColor = nova.glassBorder.copy(alpha = 0.6f)
                                )
                            )

                            state.numberSearchResult?.let { res ->
                                Spacer(modifier = Modifier.height(10.dp))
                                val isSpam = res.isSpam
                                val isBiz = res.category == CallerCategory.BUSINESS || res.category == CallerCategory.DELIVERY
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (isSpam) nova.missedContainer.copy(alpha = 0.65f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSpam) nova.missed.copy(alpha = 0.65f) else nova.glassBorder.copy(alpha = 0.6f)
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = when {
                                                    isSpam -> Icons.Filled.Warning
                                                    isBiz -> Icons.Filled.Business
                                                    else -> Icons.Filled.VerifiedUser
                                                },
                                                contentDescription = null,
                                                tint = if (isSpam) nova.missed else Color(0xFF0087FF),
                                                modifier = Modifier.size(22.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = res.displayName?.takeIf { it.isNotBlank() }
                                                        ?: if (isBn) "অচেনা কলার" else "Unknown Caller",
                                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = buildString {
                                                        append(if (isBn) res.category.labelBn else res.category.labelEn)
                                                        if (res.carrierOrRegion.isNotBlank()) {
                                                            append(" • ")
                                                            append(res.carrierOrRegion)
                                                        }
                                                        if (res.spamReports > 0L) {
                                                            append(" • ${res.spamReports}x spam")
                                                        }
                                                    },
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = if (isSpam) nova.missed else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(10.dp))
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            OutlinedButton(
                                                onClick = { onCallRoute(state.numberSearchQuery, CallRoute.SIM_1, res.displayName) },
                                                shape = RoundedCornerShape(10.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(15.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(if (isBn) "কল" else "Call", style = MaterialTheme.typography.labelSmall)
                                            }
                                            OutlinedButton(
                                                onClick = { onOpenProfile(state.numberSearchQuery, res.displayName) },
                                                shape = RoundedCornerShape(10.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(15.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(if (isBn) "প্রোফাইল" else "Profile", style = MaterialTheme.typography.labelSmall)
                                            }
                                            OutlinedButton(
                                                onClick = { onOpenTagCaller(state.numberSearchQuery, res.displayName) },
                                                shape = RoundedCornerShape(10.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Text(if (isBn) "নাম ট্যাগ" else "Tag ID", style = MaterialTheme.typography.labelSmall)
                                            }
                                            OutlinedButton(
                                                onClick = {
                                                    onReportSpam(
                                                        state.numberSearchQuery,
                                                        res.displayName ?: "Spam Caller",
                                                        true
                                                    )
                                                },
                                                shape = RoundedCornerShape(10.dp),
                                                border = BorderStroke(0.8.dp, nova.missed.copy(alpha = 0.65f)),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Icon(Icons.Filled.Flag, contentDescription = null, tint = nova.missed, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = if (isBn) "স্প্যাম রিপোর্ট" else "Report Spam",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = nova.missed
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. Period Segmented Switcher: Daily | Weekly | Monthly
                item(key = "period_tabs") {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(50),
                        color = nova.glassSurface,
                        border = BorderStroke(1.dp, nova.glassBorder.copy(alpha = 0.72f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            AnalyticsPeriod.entries.forEach { period ->
                                val selected = state.analyticsPeriod == period
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(50))
                                        .background(
                                            if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                                        )
                                        .clickable { onSelectPeriod(period) }
                                        .padding(vertical = 9.dp)
                                        .testTag("period_tab_${period.name.lowercase()}"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (isBn) period.labelBn else period.labelEn,
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. 2x2 Summary Metric Cards
                item(key = "metrics_grid") {
                    val talkStr = formatCallDuration(summary.totalDurationSeconds, state.preferences.language)
                        .ifEmpty { if (isBn) "০ মি" else "0m" }
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            DashboardMetricCard(
                                title = if (isBn) "মোট কল" else "Total Calls",
                                value = "${summary.totalCalls}",
                                subtitle = if (isBn) "সময়: $talkStr" else "Talk: $talkStr",
                                icon = Icons.Filled.Call,
                                accent = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                            DashboardMetricCard(
                                title = if (isBn) "আউটগোয়িং" else "Outgoing",
                                value = "${summary.outgoingCalls}",
                                subtitle = if (isBn) "ডায়াল করা কল" else "Placed calls",
                                icon = Icons.AutoMirrored.Filled.CallMade,
                                accent = nova.sim1,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            DashboardMetricCard(
                                title = if (isBn) "ইনকামিং" else "Incoming",
                                value = "${summary.incomingCalls}",
                                subtitle = if (isBn) "রিসিভড কল" else "Received calls",
                                icon = Icons.AutoMirrored.Filled.CallReceived,
                                accent = nova.sim2,
                                modifier = Modifier.weight(1f)
                            )
                            DashboardMetricCard(
                                title = if (isBn) "মিসড কল" else "Missed",
                                value = "${summary.missedCalls}",
                                subtitle = if (isBn) "ব্লকড: ${summary.blockedCount}" else "Blocked: ${summary.blockedCount}",
                                icon = Icons.AutoMirrored.Filled.CallMissed,
                                accent = nova.missed,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // 4. Visual Stacked Bar Chart
                item(key = "chart_card") {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        color = nova.glassSurface,
                        border = BorderStroke(1.dp, nova.glassBorder.copy(alpha = 0.72f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = if (isBn) "কল অ্যাক্টিভিটি চার্ট" else "Call Activity Trend",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                LegendDot(color = nova.sim1, label = if (isBn) "আউটগোয়িং" else "Outgoing")
                                LegendDot(color = nova.sim2, label = if (isBn) "ইনকামিং" else "Incoming")
                                LegendDot(color = nova.missed, label = if (isBn) "মিসড" else "Missed")
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            CallTrendBarChart(
                                buckets = summary.buckets,
                                outgoingColor = nova.sim1,
                                incomingColor = nova.sim2,
                                missedColor = nova.missed,
                                trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
                            )
                        }
                    }
                }

                // 5. Dual SIM Distribution & Spam Protection Status
                item(key = "sim_distribution") {
                    val simTotal = (summary.sim1Calls + summary.sim2Calls).coerceAtLeast(1)
                    val sim1Fraction = summary.sim1Calls.toFloat() / simTotal.toFloat()
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        color = nova.glassSurface,
                        border = BorderStroke(1.dp, nova.glassBorder.copy(alpha = 0.72f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Filled.SimCard, contentDescription = null, tint = nova.gold, modifier = Modifier.size(19.dp))
                                    Text(
                                        text = if (isBn) "ডুয়াল সিম ব্যবহার ও সুরক্ষা" else "Dual SIM Ratio & Protection",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${state.sim1.displayName}: ${summary.sim1Calls}",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = nova.sim1
                                )
                                Text(
                                    text = "${state.sim2.displayName}: ${summary.sim2Calls}",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = nova.sim2
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(12.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(nova.sim2.copy(alpha = 0.85f))
                            ) {
                                if (summary.sim1Calls > 0 || summary.sim2Calls == 0) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(sim1Fraction.coerceIn(0.05f, 1f))
                                            .height(12.dp)
                                            .background(nova.sim1)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = nova.glassBorder.copy(alpha = 0.45f))
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Shield,
                                    contentDescription = null,
                                    tint = if (state.preferences.spamProtectionEnabled) nova.sim1 else nova.missed,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = if (state.preferences.spamProtectionEnabled) {
                                        if (isBn) "স্মার্ট স্প্যাম প্রোটেকশন চালু আছে • ${summary.blockedCount} টি নম্বর ব্লকড"
                                        else "Smart Spam Protection Active • ${summary.blockedCount} numbers blocked"
                                    } else {
                                        if (isBn) "স্প্যাম অটো-প্রোটেকশন বন্ধ আছে (সেটিংস থেকে অন করুন)"
                                        else "Spam auto-protection is off (enable in Settings)"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // 6. Top Frequent Contacts
                if (summary.topContacts.isNotEmpty()) {
                    item(key = "top_contacts_hdr") {
                        Text(
                            text = if (isBn) "সবচেয়ে বেশি কথা বলা কন্টাক্ট" else "Top Frequent Contacts",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    items(summary.topContacts, key = { it.number }) { stat ->
                        val talk = formatCallDuration(stat.totalDurationSeconds, state.preferences.language)
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .clickable { onOpenProfile(stat.number, stat.displayName) },
                            shape = RoundedCornerShape(18.dp),
                            color = nova.glassSurface,
                            border = BorderStroke(0.8.dp, nova.glassBorder.copy(alpha = 0.6f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ContactAvatar(name = stat.displayName, photoUri = stat.photoUri, size = 40.dp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stat.displayName,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = buildString {
                                            append(if (isBn) "${stat.callCount} বার কল" else "${stat.callCount} calls")
                                            if (talk.isNotEmpty()) {
                                                append(" • ")
                                                append(talk)
                                            }
                                            append(" • ")
                                            append(formatDisplayNumber(stat.number))
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(nova.sim1Container)
                                        .clickable { onCallRoute(stat.number, CallRoute.SIM_1, stat.displayName) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.Call, contentDescription = "Call", tint = nova.sim1, modifier = Modifier.size(17.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardMetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val nova = LocalNovaColors.current
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = nova.glassSurface,
        border = BorderStroke(1.dp, nova.glassBorder.copy(alpha = 0.72f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(15.dp))
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CallTrendBarChart(
    buckets: List<AnalyticsChartBucket>,
    outgoingColor: Color,
    incomingColor: Color,
    missedColor: Color,
    trackColor: Color
) {
    if (buckets.isEmpty()) return
    val maxTotal = remember(buckets) { (buckets.maxOfOrNull { it.total } ?: 1).coerceAtLeast(1) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
        ) {
            val count = buckets.size
            val slotWidth = size.width / count.toFloat()
            val barWidth = (slotWidth * 0.42f).coerceIn(10.dp.toPx(), 28.dp.toPx())
            val chartHeight = size.height

            buckets.forEachIndexed { index, b ->
                val x = index * slotWidth + (slotWidth - barWidth) / 2f
                drawRoundRect(
                    color = trackColor,
                    topLeft = Offset(x, 0f),
                    size = Size(barWidth, chartHeight),
                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                )

                if (b.total > 0) {
                    val outH = (b.outgoingCount.toFloat() / maxTotal.toFloat()) * chartHeight
                    val inH = (b.incomingCount.toFloat() / maxTotal.toFloat()) * chartHeight
                    val missH = (b.missedCount.toFloat() / maxTotal.toFloat()) * chartHeight
                    var cursorY = chartHeight

                    if (outH > 0f) {
                        cursorY -= outH
                        drawRoundRect(
                            color = outgoingColor,
                            topLeft = Offset(x, cursorY),
                            size = Size(barWidth, outH),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )
                    }
                    if (inH > 0f) {
                        cursorY -= inH
                        drawRoundRect(
                            color = incomingColor,
                            topLeft = Offset(x, cursorY),
                            size = Size(barWidth, inH),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )
                    }
                    if (missH > 0f) {
                        cursorY -= missH
                        drawRoundRect(
                            color = missedColor,
                            topLeft = Offset(x, cursorY.coerceAtLeast(0f)),
                            size = Size(barWidth, missH),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            buckets.forEach { b ->
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = b.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                    if (b.total > 0) {
                        Text(
                            text = "${b.total}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
